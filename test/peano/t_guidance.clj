(ns peano.t-guidance
  (:require [clojure.core.logic :as l]
            [clojure.zip :as zip])
  (:use midje.sweet
        [clojure.set :only [difference]]
        clojure.pprint
        peano.guidance))

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



