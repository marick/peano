(ns logic.t-core
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        logic.core))

(data [animal :by :name]
      {:name "betty" :species :bovine}
      {:name "hank" :species :equine})

(fact "data statements generate relations and facts"
  (l/run* [q] (animal-name-o q)) => (just "hank" "betty" :in-any-order)
  (l/run* [q] (animal-name-o "hank")) =not=> empty?
  (l/run* [q] (animal-species-o q :bovine)) => (just "betty"))

(fact "data accessors are created"
   (animal-data?? "betty") => {:name "betty" :species :bovine}
   (animal-data?? "NOT PRESENT") => nil
   (animal-data??) =future=> (just {:name "betty" :species :bovine}
                                 {:name "hank" :species :equine}))

(defchecker betty-or-hank [actual]
  (some #{actual} ["hank" "betty"])) 

(future-fact "name selectors are created"
  (animal-name?? "betty") => "betty"
  (one (animal-name??)) => betty-or-hank
  (n 2 (animal-name??)) => (just "hank" "betty" :in-any-order)
  (animal-name??) => (just "hank" "betty" :in-any-order))

(future-fact "other selectors are created"
  (animal-species?? :name "betty" :species :bovine) => truthy
  (animal-species?? :name "betty") => [:bovine]
  (animal-species?? :species :bovine) => ["betty"]
  (animal-species??) => ["betty"]


;; Bindings are obeyed
(let [value 33
      who "betty"]
  (data [bindinged :by :name]
        {:name who :species value :fun odd?}
        {:name "hank" :species (+ 1 value) :fun even?}))

(fact "let bindings are obeyed"
  (l/run* [q] (bindinged-name-o q)) => (just "hank" "betty" :in-any-order)
  (l/run* [q] (bindinged-species-o q 33)) => (just "betty")
  (l/run* [q] (bindinged-species-o "hank" q)) => (just 34)
  ((first (l/run* [q] (bindinged-fun-o "hank" q))) 34) => true
  (bindinged-data "betty") => {:name "betty" :species 33 :fun odd?})

