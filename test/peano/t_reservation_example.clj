(ns peano.t-reservation-example
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        [clojure.math.combinatorics :only [combinations]]
        peano.sweet
        peano.guidance
        peano.tokens
        [peano.blank-filling :only [suggested-classifier generate-run-form]]))

(defmulti processor
  (fn [guidance blank & _] ((:classifier guidance) blank)))

(defn make-and-install-new-lvar [guidance lvar-type]
  (let [[guidance lvar] (with-new-lvar guidance lvar-type)
        guidance (assoc-into-vector guidance lvar-type lvar)]
    [guidance lvar]))

;; TODO: This is really asking for something state-monad-like
(defmethod processor :unconstrained-blank [guidance _ lvar-type]
  (make-and-install-new-lvar guidance lvar-type))

(defmethod processor :presupplied-lvar [guidance lvar lvar-type]
  (vector (-> guidance
              (with-lvar lvar)
              (assoc-into-vector lvar-type lvar))
          lvar))

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

(defmethod processor :blank-that-identifies [guidance identifier lvar-type]
  (make-and-install-new-lvar-with-properties guidance lvar-type {:name identifier}))           

(defn simplify-and-process [guidance blank _ count-to-left]
  (processor guidance blank
             (if (= 0 count-to-left) :animal :procedure)))

(defn permitted-pairs-narrowers [guidance]
  (map (fn [procedure animal] `(permitted?? ~procedure ~animal))
       (guidance :procedure)
       (guidance :animal)))

(defn no-duplicate-groups-narrowers [uses]
  (map (fn [[one two]] `(l/!= ~one ~two))
       (combinations uses 2)))

(fact "remove duplicate groups"
  (no-duplicate-groups-narrowers [ [1 1] ]) => []
  (no-duplicate-groups-narrowers [ [1 1] [2 2] [3 3]])
  => (just '(clojure.core.logic/!= [1 1] [2 2])
           '(clojure.core.logic/!= [1 1] [3 3])
           '(clojure.core.logic/!= [2 2] [3 3])
           :in-any-order))

(defn postprocessor [guidance tree]
  (let [narrowers (concat (permitted-pairs-narrowers guidance)
                          (no-duplicate-groups-narrowers tree))
        ]
    (vector (merge-with concat guidance {:narrowers narrowers})
            tree)))

(def guidance {:classifier suggested-classifier
               :processor simplify-and-process
               :postprocessor postprocessor})

;;; About processing

;;; Note: I'm avoiding prerequisites to make these tests easier to read for the
;;; not-Midje-initiated.

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
     

(fact "a named lvar is used instead of a generated one"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  'my-procedure
                                                  ..irrelevant.. 1)]
    lvar => 'my-procedure
    new-guidance => (contains {:lvars-needed [lvar], :procedure [lvar]})))
     

(fact "a property map generates an lvar and constrains it"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  {:prop 'val :and 'val2}
                                                  ..irrelevant.. 0)
        prop-narrower (property-narrower [:animal :prop] lvar 'val)
        and-narrower (property-narrower [:animal :and] lvar 'val2)]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [prop-narrower and-narrower]})))

(fact "a string constrains the name"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  "betsy"
                                                  ..irrelevant.. 0)
        narrower (property-narrower [:animal :name] lvar "betsy")]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [narrower]})))
  
  

(data [animal :by :name :make-selectors]
      {:name "betty" :species :bovine :legs 4}
      {:name "julie" :species :bovine :legs 4}
      {:name "jeff" :species :equine :legs 4}
      {:name "hank" :species :equine :legs 3}) ; poor hank

(data [procedure :by :name :make-selectors]
      {:name "hoof trim" :species :equine :days-delay 0}
      {:name "casting teeth" :species :equine :days-delay 0}
      {:name "superovulation" :species :bovine :days-delay 90})

(defn permitted?? [procedure animal]
  (l/fresh [species]
         (procedure-species?? procedure species)
         (animal-species?? animal species)))

(defn generate-forest-filler-run-form
  ([run-count guidance tree]
     (apply (partial generate-run-form run-count)
            (fill-in-the-blanks guidance (vec tree))))
  ([guidance tree]
     (if (number? (first tree))
       (generate-forest-filler-run-form (first tree) guidance (rest tree))
       (generate-forest-filler-run-form false        guidance tree ))))

(defn generate-one-forest-filler-form [guidance tree]
  `(first ~(generate-forest-filler-run-form 1 guidance tree)))
  
(defn def-forest-fillers* [basename guidance]
  (let [selector (selector-symbol basename)]
    `(do
       (defmacro ~selector [& tree#]
         (generate-forest-filler-run-form guidance tree#))
       (defmacro ~(one-selector-symbol basename) [& tree#]
         (generate-one-forest-filler-form guidance tree#)))))

(defmacro def-forest-fillers [basename guidance]
  (def-forest-fillers* basename guidance))

(def-forest-fillers reservation guidance)

(fact "simple ones" 
  ;; (println "============== SIMPLE")
  ;; (pprint (reservations [- -] [- -] [- -]))
  (let [result (reservation?> [- -] [- -] [- -])]
    (> (count result) 20) => truthy
    result => (contains [[["julie" "superovulation"] ["hank" "hoof trim"] ["betty" "superovulation"]]])))
  
(fact "more complex ones" 
  ;; (println "============== BIG")
  ;; (pprint (reservation?> ["hank" -] [{:species :bovine} -]))
  (let [result (reservation?> ["hank" -] [{:species :bovine} -])]
    (count result) => 4
    result => (just [[["hank" "casting teeth"] ["betty" "superovulation"]]
                     [["hank" "hoof trim"] ["betty" "superovulation"]]
                     [["hank" "casting teeth"] ["julie" "superovulation"]]
                     [["hank" "hoof trim"] ["julie" "superovulation"]]]
                    :in-any-order)))

(fact "can limit count"
  (let [result (reservation?> 2 ["hank" -] [{:species :bovine} -])]
    (count result) => 2))

(fact "there is a 'one' macro"
  (let [[[animal0 procedure0] [animal1 procedure1]]
        (one-reservation?> ["hank" -] [{:species :bovine} -])]
    (some #{animal0} (animal?>)) => truthy
    (some #{procedure0} (procedure?>)) => truthy
    (some #{animal1} (animal?>)) => truthy
    (some #{procedure1} (procedure?>)) => truthy))
  
;; (pprint (reservation?> ["hank" -] [{:species :bovine} -]))
