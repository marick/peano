(ns peano.t-tokens
  (:use midje.sweet
        clojure.pprint
        peano.tokens))

(fact "keys can be converted to lvars (symbols)"
  (keys-to-lvars [:one :two]) => '(one two))

(fact "relations can be canonicalized"
  (canonicalize-relation 'relation??) => 'relation
  (canonicalize-relation "relation??") => 'relation
  (canonicalize-relation 'relation) => 'relation)
