(ns peano.t-example
  (:require [clojure.core.logic :as l])
  (:use midje.sweet peano.core))

;;; Feature: Constructing relation and query functions from tabular data

(data [animal :by :name :with-selectors]
      {:name "betty" :species :bovine :legs 4}
      {:name "hank" :species :equine :legs 4}
      {:name "jake" :species :equine :legs 3} ; poor jake
      {:name "dawn" :species :human :legs 2})

(fact "We now have a multitude of relations that can be used to find names of appropriate data"
  (l/run* [name] (animal-species?? name :equine)) => (just "hank" "jake" :in-any-order))

(fact "We also have functions that give us names without having to write the query"
  (animal?> :species :equine) => (just "hank" "jake" :in-any-order)
  (animal?> :species :equine, :legs 3) => ["jake"]
  ;; If we know we only want one animal
  (one-animal?> :species :equine, :legs 3) => "jake")


;; Feature: Query functions also work with derived relations/goals.

;; We begin with medical procedures that apply to one or more animal species.
(data [procedure :by :name :with-selectors]
      {:name "hoof trim" :species-rule :equine-only :days-delay 0}
      {:name "superovulation" :species-rule :bovine-only :days-delay 90}
      {:name "physical exam" :species-rule :all :days-delay 0})

;; We want to be able to talk about "permitted" pairings of animals and procedures,
;; but we don't yet have a direct link between animals (with their :species field)
;; and procedures (with their :species-rule field). We can derive a relation from
;; procedures that lets us talk about a procedure's species. Here's one way:
(defn procedure-species?? [name species]
  (l/fresh [species-rule]
           (l/conde ((procedure-species-rule?? name :equine-only)
                     (l/== species :equine))
                    ((procedure-species-rule?? name :bovine-only)
                     (l/== species :bovine))
                    ((procedure-species-rule?? name :all)))))

(fact "procedures can be queried in terms of species"
  (l/run* [name] (procedure-species?? name :bovine))
  => (just "physical exam" "superovulation" :in-any-order))

(fact "More importantly, the selectors work too"
  (procedure?> :species :bovine)
  => (just "physical exam" "superovulation" :in-any-order))


;;; Partially working: Test generation of this sort

(future-fact "dashes describe blanks to be filled in"
  (fill-in-the-blanks '[[- -] [- -]] guidance)
  => (contains {:filled-in [["animal-0" "procedure-0"]
                            ["animal-1" "procedure-1"]]}))

;; Note that the pairs are constrained to use procedures
;; permitted for that particular animal (via the unshown `guidance`
;; map and, currently, too much hardcoding).

(future-fact "Additional constraints can be added by making the blanks
              less blank-like"
  (fill-in-the-blanks '[["hank" -] [{:species :bovine} -]] guidance)
  => (contains {:filled-in '[["hank" "hoof trim"] ["betty" "physical exam"]]}))





