(ns peano.t-selectors
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.selectors))

(fact "given keys can be removed from allowed keys"
  (remaining-keys [:one :two :three :four] [:one :three]) => [:two :four])

(fact "Clauses to narrow can be generated from keys"
  (narrower-clauses {:one "one", :two "two"})
  => (just '(clojure.core.logic/== one "one") '(clojure.core.logic/== two "two") :in-any-order))


