(ns peano.t-core
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.core))

;;;                             Constructing data and query functions.

(data [animal :by :name]
      {:name "betty" :species :bovine}
      {:name "hank" :species :equine})

(fact "data statements generate relations and facts"
  (l/run* [q] (animal?? q)) => (just "hank" "betty" :in-any-order)
  (l/run* [q] (animal?? "hank")) =not=> empty?
  (l/run* [q] (animal-species?? q :bovine)) => (just "betty"))

(fact "For completeness, the did is also used in a binary relation"
  (l/run* [q] (animal-name?? q q)) => (just "hank" "betty" :in-any-order))

(fact "data accessors are created"
   (animal-data "betty") => {:name "betty" :species :bovine}
   (animal-data "NOT PRESENT") => nil
   (animal-data) => (just {:name "betty" :species :bovine}
                          {:name "hank" :species :equine}))

;; Bindings are obeyed
(let [value 33
      who "betty"]
  (data [bindinged :by :name]
        {:name who :species value :fun odd?}
        {:name "hank" :species (+ 1 value) :fun even?}))

(fact "let bindings are obeyed"
  (l/run* [q] (bindinged?? q)) => (just "hank" "betty" :in-any-order)
  (l/run* [q] (bindinged-species?? q 33)) => (just "betty")
  (l/run* [q] (bindinged-species?? "hank" q)) => (just 34)
  ((first (l/run* [q] (bindinged-fun?? "hank" q))) 34) => true
  (bindinged-data "betty") => {:name "betty" :species 33 :fun odd?})



;;;                             Selector functions

(make-selector-functions animal)

(fact "The list of dids can be returns"
  (animal?>) => (just "hank" "betty" :in-any-order))

(comment

  (future-fact "selectors apply to two-valued properties with all left blank"
  (animal-species?>) => (just ["hank" :equine] ["betty" :bovine] :in-any-order))

(future-fact "an unspecified field drives the results"
  (animal-species?> :species :bovine) => ["betty"]
  (animal-species?> :name "betty") => [:bovine])

(future-fact "just for fun, you can a boolean result"
  (animal-species?> :species :bovine :name "betty") => truthy
  (animal-species?> :species :bovine :name "hank") => falsey
  (animal-species?> :species :querty :name "betty") => falsey)
             
)
