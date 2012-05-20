(ns peano.t-relations
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        peano.relations))
         
(fact "Index relations"
  (index-relation 'animal-name-o :name) =>
  '(clojure.core.logic/defrel animal-name-o :name))
  
(fact "Index facts"
  (index-facts 'animal-name-o ["Bessie"])
  => '[(clojure.core.logic/fact animal-name-o "Bessie")])

(fact "Binary relations"
  (binary-relation 'animal-species-o :name :species) =>
  '(clojure.core.logic/defrel animal-species-o :name :species))
  
(fact "Binary facts"
  (binary-facts 'animal-species-o ["Bessie"] [:bovine])
  => '[(clojure.core.logic/fact animal-species-o "Bessie" :bovine)])

;; (let [foo 33]
;;   (clojure.pprint/pprint (data* 'animal [{:a 1 :b even? :c foo}])))
