(ns peano.t-default-guidance
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.default-guidance))

(fact "there is a default way of classifying leaf nodes"
  (default-classification '-) => :unconstrained-blank
  (default-classification 'other-symbol) => :presupplied-lvar
  (default-classification "string") => :blank-that-identifies
  (default-classification {:species :bovine}) => :blank-with-properties
  (default-classification '(something else)) => nil)


