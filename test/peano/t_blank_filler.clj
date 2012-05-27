(ns peano.t-blank-filler
  (:require [clojure.core.logic :as l]
            [clojure.zip :as zip])
  (:use midje.sweet
        [clojure.set :only [difference]]
        clojure.pprint
        peano.blank-filler))

(fact "you can make sure a map contains a starting value for a key"
  (with-guaranteed-key {} :key :value) => {:key :value}
  "But it doesn't overwrite"
  (with-guaranteed-key {:key :value} :key :ignored) => {:key :value}
  "Default is an empty vector"
  (with-guaranteed-key {} :key) => {:key []})

(fact "you can associate into a vector"
  (let [one (assoc-into-vector {} :key 1)
        two (assoc-into-vector one :key 2)]
    one => {:key [1]}
    two => {:key [1 2]}))
  

(fact "lvars can be stashed"
  (let [guidance (with-lvar {} 'symbol)]
    guidance => {:lvars-needed '[symbol]}
    "But they aren't entered twice"
    (with-lvar guidance 'symbol) => {:lvars-needed '[symbol]}))

(fact "lvars can be created"
  (let [[guidance0 newvar0] (with-new-lvar {} :animal)
        [guidance1 newvar1] (with-new-lvar guidance0 :animal)
        [guidance2 newvar2] (with-new-lvar guidance1 :procedure)]
    newvar0 => 'animal-0
    newvar1 => 'animal-1
    newvar2 => 'procedure-0
    (:lvars-needed guidance0) => '[animal-0]
    (:lvars-needed guidance1) => '[animal-0 animal-1]
    (:lvars-needed guidance2) => '[animal-0 animal-1 procedure-0]))

(fact "you can write a logic clause that forces an lvar to associate with a property"
  (property-narrower [:animal :name] 'animal-0 "bessy")
  => '(clojure.core.logic/== (animal-name?? animal-0 "bessy")))

(fact "narrowers can be added into the guidance"
  (with-narrower {:key 1} '(a narrower)) => {:key 1, :narrowers '[(a narrower)]})


(fact "there is a default way of classifying leaf nodes"
  (default-classification '-) => :unconstrained-blank
  (default-classification 'other-symbol) => :presupplied-lvar
  (default-classification "string") => :blank-that-identifies
  (default-classification {:species :bovine}) => :blank-with-properties
  (default-classification '(something else)) => nil)




(comment
  
(fact "trivial case produces no change"
  (fill-in-the-blanks '[] guidance)
  => (contains {:filled-in []}))

(fact "dashes describe blanks to be filled in"
  (fill-in-the-blanks '[-] guidance)
  => (contains {:filled-in ["animal-0"], :logic-vars-needed ["animal-0"]})

  (fill-in-the-blanks '[[- -] [- -]] guidance)
  => (contains {:filled-in [["animal-0" "procedure-0"] ["animal-1" "procedure-1"]],
                :logic-vars-needed ["animal-0" "procedure-0" "animal-1" "procedure-1"]}))


(fact "logical variables can be specified"
  (fill-in-the-blanks '[- b] guidance)
  => (contains {:filled-in '["animal-0" b]
                :logic-vars-needed '["animal-0" b]}))

(fact "strings name the 'did' of a generated variable, thus restricting it"
  (fill-in-the-blanks '["hank" b] guidance)
  => (contains {:filled-in '["animal-0" b]
                :logic-vars-needed '["animal-0" b]
                :extra-restrictions '["hank"]}))

)
