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



;;;                             DId selectors

(data [procedure :by :name]
      {:name "hoof trim" :species-rule :equine-only :days-delay 0}
      {:name "superovulation" :species-rule :bovine-only :days-delay 90}
      {:name "physical exam" :species-rule :all :days-delay 0})

(make-did-selector procedure)

(fact "The list of dids can be returned"
  (procedure?>) => (just "hoof trim" "superovulation" "physical exam" :in-any-order))

(fact "a key-value pair can narrow the returned results"
  (procedure?> :species-rule :bovine-only) => ["superovulation"]
  (procedure?> :species-rule :travelogue) => []
  (procedure?> :days-delay 0) => ["hoof trim" "physical exam"]
  (procedure?> :name "physical exam") => ["physical exam"]) ; kind of silly

(fact "more than one field can be used"
  (procedure?> :species-rule :bovine-only :days-delay 90) => ["superovulation"]
  (procedure?> :species-rule :bovine-only :days-delay 900000) => []
  (procedure?> :species-rule :human :days-delay 90) => [])

(fact "bindings are obeyed"
  (let [days-delay 90]
    (procedure?> :species-rule :bovine-only :days-delay days-delay) => ["superovulation"]))

(fact "can use a map argument instead of a list of key-values"
  (procedure?> {:species-rule :bovine-only}) => ["superovulation"])
  

(l/defrel species-breakdown?? :rule        :species)
(l/fact   species-breakdown?? :bovine-only :bovine) 
(l/fact   species-breakdown?? :equine-only :equine) 
(l/fact   species-breakdown?? :all         :bovine) 
(l/fact   species-breakdown?? :all         :equine) 

(defn procedure-species?? [name species]
  (l/fresh [species-rule]
           (procedure-species-rule?? name species-rule)
           (species-breakdown?? species-rule species)))

(fact "did-selectors work with derived relations"
  (procedure?> :species :bovine) => (just "superovulation" "physical exam" :in-any-order))

(fact "can limit the number of returned dids"
  (procedure?> 1 :species :bovine) => #(some #{%} [["superovulation"] ["physical exam"]]))
