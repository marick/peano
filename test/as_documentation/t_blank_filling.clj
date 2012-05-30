(ns as-documentation.t-blank-filling
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        peano.sweet
        clojure.pprint
        [clojure.math.combinatorics :only [combinations]]
        [peano.guidance :only [with-new-lvar assoc-into-vector
                               with-narrower property-narrower]]))

;;; Context: A *reservation* consists of a set of pairs of animal and
;;; procedure names. For example, you might reserve the horse Hank
;;; for a foot trim and the cow Betsy for "superovulation".
;;;
;;; We might represent such a reservation like this:
;;;
;;;  [["hank" "hoof trim"] ["betty" "superovulation"]]
;;;
;;; Test data is typically subject to constraints. For example, in
;;; this case, some procedures are permitted only for particular
;;; species. (You cannot superovulate a horse.)
;;;
;;; Constraints complicate testing. One approach is to start with a
;;; bunch of preselected animals and procedures with various
;;; properties. A tester wanting to write a new reservation test
;;; grovels through the data to find animals and procedures that have
;;; properties appropriate for the test and also satisfy all
;;; constraints. A reservation is constructed for them and then used
;;; in the test.
;;;
;;; That's fine until someone discovers there's no animal quite right
;;; for her test. She can change an existing animal, but that might
;;; break existing tests. She can add a new animal, but (as the number
;;; of animals grows), you have a documentation problem: you have to
;;; explain what's special about each animal.
;;;
;;; Things are worse when constraints in the system change, as they
;;; can break old tests in ways that are hard to debug.
;;;
;;;
;;; Another approach to the new reservation test is to construct
;;; appropriate animals, procedures, and reservations on the fly. This
;;; is awkward. You might end up with a lot of data construction code
;;; that obscures the point of the test. It's not just the sheer
;;; volume of code: it's also that the code typically speaks of
;;; details that are irrelevant to the purpose of the test. Not only
;;; do those details make the code harder to read, they are also
;;; sources of fragility as the data constraints evolve.
;;;
;;; Rather than a bunch of test-specific code, you might end up with
;;; an elaborate and ad-hoc test data generation framework. These
;;; always seem to be harder to write and update and use than you
;;; expected.

;;; The approach of this proof of concept differs. We start with
;;; predefined sets of "atomic" data like animals or procedures, but
;;; no human picks elements of that data. When needing a reservation,
;;; the tester specifies properties that the reservation (and its
;;; constituent animals and procedures) needs to satisfy. A logic
;;; engine selects animals and procedures that make up a matching
;;; reservation.

;;; For example, it's quite common just to want a reservation with a
;;; single animal/procedure pair. That would look like this:

;;;     (reservation?> [- -])
;;;
;;; The dashes represent "don't care" conditions. The logic engine
;;; will fill in one procedure and one animal. Alternately, the user
;;; might want a cow whose "days delay" property is 3:

;;;     (reservation?> [{:days-delay 3, :species :bovine} -])
;;;
;;; This will find an appropriate cow (or fail if there is no such
;;; cow) and match it with a permitted procedure. The map says "Fill
;;; in the blank with an animal with these properties."

;;; Let's work through how `reservation?>` gets written. We can start
;;; with animals and procedures:

(data [animal :by :name :make-selectors]
      {:name "betty" :species :bovine :legs 4}
      {:name "julie" :species :bovine :legs 4}
      {:name "jeff" :species :equine :legs 4}
      {:name "hank" :species :equine :legs 3}) ; poor hank

(data [procedure :by :name :make-selectors]
      {:name "hoof trim" :species :equine :days-delay 0}
      {:name "casting teeth" :species :equine :days-delay 0}
      {:name "superovulation" :species :bovine :days-delay 90})

;;; These definitions create various relations. For example, the
;;; following represents all animals that are cows:

;;;    (l/fresh [animal] (animal-species?? animal :bovine))

;;; We also need to define the "permitted" constraint. This is done
;;; with core.logic. I hope that such constraints won't require a great
;;; deal of logic programming skill.

(defn permitted?? [procedure animal]
  (l/fresh [species]
         (procedure-species?? procedure species)
         (animal-species?? animal species)))

;;; This identifies all procedure/animal pairs satisfying the
;;; constraint that each pair's procedure and animal have the same
;;; species.

;;; We next need to set up the "guidance" map, which controls the way
;;; blanks are filled in. It contains three functions.

;;; The first (the *classifier*) identifies which kind of forms are
;;; which kind of blanks.  blanks (in the way described above).

(defn classifier [form]
  (cond (= '- form) :unconstrained-blank
        (map? form) :blank-with-properties))

;;; The second function (the *processor*) replaces a blank with the
;;; name of a matching animal or procedure. It takes the guidance map
;;; (and returns a new version), a form classified as a blank, and the
;;; form's position in the nested structure to be filled in. This
;;; position is a position in a tree: the number of levels above the
;;; form, and the number of forms to its left. In this case, all we
;;; care about is the second. The first element in a row is an animal;
;;; the next is a procedure.

;;; In our case, the processor function immediately delegates to a
;;; helper multimethod with a simpler arglist. There's one multimethod
;;; for dashes and one for maps:

(defmulti processor
  (fn [guidance blank lvar-type] ((guidance :classifier) blank)))

;;; The "lvar-type" is either :animal or :procedure. 

;;; We'll first look at dash handling. It uses two helper
;;; functions. The first, given :animal, produces a series of logical
;;; variables (symbols) of this form: animal-0, animal-1, ... It
;;; stashes the counter inside the guidance. The logical variable is returned and used
;;; appropriately by the logic engine. (You don't need to know how to use this code.)
;;;
;;; The second stashes the new variable inside the guidance, using the
;;; lvar-type as a key. That is, over time, the `:animal` key will
;;; have a value like this: [animal-0, animal-1, ...] You'll see how that's used later. 

(defn make-and-install-new-lvar [guidance lvar-type]
  (let [[guidance lvar] (with-new-lvar guidance lvar-type)
        guidance (assoc-into-vector guidance lvar-type lvar)]
    [guidance lvar]))

;;; Note: something like the state monad should be used to hide the
;;; passing of guidance around.

(defmethod processor :unconstrained-blank [guidance _ lvar-type]
  (make-and-install-new-lvar guidance lvar-type))

;;; Handling a map is slightly more complicated. It does the same work
;;; as in the dash case, but also uses a canned "narrower" function
;;; that converts something like {:species :bovine} in the :animal
;;; position into:
;;;    (animal-species?? animal-0 :bovine)
;;; This new logical clause is stashed in the guidance map. 

(defn make-and-install-new-lvar-with-properties [guidance lvar-type property-map]
   (let [[guidance lvar] (make-and-install-new-lvar guidance lvar-type)
         guidance (reduce (fn [guidance [key value]]
                            (with-narrower guidance (property-narrower [lvar-type key]
                                                                       lvar value)))
                          guidance
                          property-map)]
     [guidance lvar]))

(defmethod processor :blank-with-properties [guidance property-map lvar-type]
   (make-and-install-new-lvar-with-properties guidance lvar-type property-map))

;;; Here is the function that converts the engine's default arguments into the simpler form:

(defn simplify-and-process [guidance blank _ count-to-left]
  (processor guidance blank
             (if (= 0 count-to-left) :animal :procedure)))

;;; The `processor` step handles constraints on a single "blank". But
;;; there are two constraints between blanks.

;;; First, within a [animal procedure] pair, the procedure must be
;;; permitted on that animal. That means the addition of pairs like
;;; this:
;;;   (permitted?? procedure-0 animal-0)
;;;   (permitted?? procedure-1 animal-1)
;;;
;;; This is easily done because we have an :animal and a :procedure
;;; vector in the guidance. That was done to avoid having to grovel over
;;; the tree to pick out the pairs. 

(defn permitted-pairs-narrowers [guidance]
  (map (fn [procedure animal] `(permitted?? ~procedure ~animal))
       (guidance :procedure)
       (guidance :animal)))

;;; As it turns out, though, we have to do that groveling for another
;;; constraint. Consider this structure: [ [- -] [- -] ]. It's valid
;;; (and common) to have duplicate procedures or animals, like this:
;;;    ["betsy" "superovulation"] ["betsy" "physical exam"]
;;;    ["jake"  "physical exam"]  ["betsy" "physical exam"]
;;;
;;; This, however, would be silly:
;;;    ["betsy" "superovulation"] ["betsy" "superovulation"]
;;;
;;; This is prohibited by adding `(/!= pair1 pair2)` for every
;;; combination of pairs.

(defn no-duplicate-groups-narrowers [uses]
  (map (fn [[one two]] `(l/!= ~one ~two))
       (combinations uses 2)))

;;; The postprocessor adds all the narrowers constructed in these two
;;; ways:

(defn postprocessor [guidance tree]
  (let [narrowers (concat (permitted-pairs-narrowers guidance)
                          (no-duplicate-groups-narrowers tree))]
    (vector (merge-with concat guidance {:narrowers narrowers})
            tree)))

;;; The guidance map is constructed:

(def guidance {:classifier suggested-classifier
               :processor simplify-and-process
               :postprocessor postprocessor})

;;; A "forest selector" takes a number of vectors with blanks to be
;;; filled in. It constructs a logic expression that causes the blanks
;;; to be filled in with procedure or animal names (as appropriate),
;;; runs it, and reports the results.
;;;
;;; (Note: you may be wondering what we did that filled in the blanks
;;; with animal names. It's `permitted?`. It associates a logical
;;; variable with the first argument of `animal-species??` or
;;; `procedure-species??`. That first argument represents a name.

(make-forest-selector reservation guidance)

;; (println "============== Simple")
;; (pprint (reservation?> [- -] [- -] [- -]))

;; (println "============== More complex")
;; (pprint (reservation?> [{:name "hank"} -] [{:species :bovine} -]))

;; (println "============== Limiting the count")
;; (pprint (reservation?> 2 [{:name "hank"} -] [{:species :bovine} -]))

;; (println "============== A single element (instead of an array of same)")
;; (pprint (one-reservation?> [{:name "hank"} -] [{:species :bovine} -]))



;;; Tests

;;; Note: I'm avoiding prerequisites to make these tests easier to read for the
;;; not-Midje-initiated.

(fact "remove duplicate groups"
  (no-duplicate-groups-narrowers [ [1 1] ]) => []
  (no-duplicate-groups-narrowers [ [1 1] [2 2] [3 3]])
  => (just '(clojure.core.logic/!= [1 1] [2 2])
           '(clojure.core.logic/!= [1 1] [3 3])
           '(clojure.core.logic/!= [2 2] [3 3])
           :in-any-order))

(fact "an unqualified first-position blank adds a procedure lvar"
  (let [[new-guidance lvar] (simplify-and-process guidance '- ..irrelevant.. 1)]
    lvar => 'procedure-0
    new-guidance => (contains {:lvars-needed [lvar], :procedure [lvar]})))
     
(fact "an unqualified second-position blank adds an animal lvar"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  '-
                                                  ..irrelevant.. 0)]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]})))
     

(fact "a property map generates an lvar and constrains it"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  {:prop 'val :and 'val2}
                                                  ..irrelevant.. 0)
        prop-narrower (property-narrower [:animal :prop] lvar 'val)
        and-narrower (property-narrower [:animal :and] lvar 'val2)]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [prop-narrower and-narrower]})))

  


(fact "simple ones" 
  (let [result (reservation?> [- -] [- -] [- -])]
    (> (count result) 20) => truthy
    result => (contains [[["julie" "superovulation"] ["hank" "hoof trim"] ["betty" "superovulation"]]])))
  
(fact "more complex ones" 
  (let [result (reservation?> [{:name "hank"} -] [{:species :bovine} -])]
    (count result) => 4
    result => (just [[["hank" "casting teeth"] ["betty" "superovulation"]]
                     [["hank" "hoof trim"] ["betty" "superovulation"]]
                     [["hank" "casting teeth"] ["julie" "superovulation"]]
                     [["hank" "hoof trim"] ["julie" "superovulation"]]]
                    :in-any-order)))

(fact "can limit count"
  (let [result (reservation?> 2 [{:name "hank"} -] [{:species :bovine} -])]
    (count result) => 2))

(fact "there is a 'one' macro"
  (let [[[animal0 procedure0] [animal1 procedure1]]
        (one-reservation?> [{:name "hank"} -] [{:species :bovine} -])]
    (some #{animal0} (animal?>)) => truthy
    (some #{procedure0} (procedure?>)) => truthy
    (some #{animal1} (animal?>)) => truthy
    (some #{procedure1} (procedure?>)) => truthy))
  
