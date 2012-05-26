(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))

(defn- pairify-arglist [flat-seq-or-seqed-map]
  (if (map? (first flat-seq-or-seqed-map))
    (into [] (first flat-seq-or-seqed-map))
    (partition 2 flat-seq-or-seqed-map)))

;; -- 
(defn generate-did-run-form
  ([run-count relation kvs]
     (let [q (gensym "q")
           pairs (pairify-arglist kvs)
           selector-for-did `(~(query-symbol relation) ~q)
           narrowing-selectors (map (fn [[key value]]
                                      `( ~(query-symbol relation key) ~q ~value))
                                    pairs)]
       `(l/run ~run-count [~q] ~selector-for-did ~@narrowing-selectors)))
  ([relation kvs]
     (if (number? (first kvs))
       (generate-did-run-form (first kvs) relation (rest kvs))
       (generate-did-run-form false       relation kvs ))))

(defn generate-seq-run-form
  ([run-count relation kvs keys]
     (let [q (gensym "q")
           pairs (pairify-arglist kvs)
           selector-for-result `(l/== [~@(keys-to-lvars keys)] ~q)
           relation-query `(~(query-symbol relation) ~@(keys-to-lvars keys))
           narrowing-selectors (map (fn [[key value]]
                                      `(l/== ~(key-to-lvar key) ~value))
                                    pairs)]
       `(l/run ~run-count [~q]
               (l/fresh [~@(keys-to-lvars keys)]
                        ~selector-for-result ~relation-query ~@narrowing-selectors))))
  ([relation kvs keys]
     (if (number? (first kvs))
       (generate-seq-run-form (first kvs) relation (rest kvs) keys)
       (generate-seq-run-form false       relation kvs        keys))))

;;- 
(defn generate-one-did-form [relation kvs]
  `(first ~(generate-did-run-form 1 relation kvs)))

(defn generate-one-seq-form [relation keys kvs]
  `(first ~(generate-seq-run-form 1 relation keys kvs)))

;; -
(defn make-did-selector* [relation]
  (let [selector (selector-symbol relation)]
    `(do
       (defmacro ~selector [& kvs#]
         (generate-did-run-form '~relation kvs#))
       (defmacro ~(one-selector-symbol relation) [& kvs#]
         (generate-one-did-form '~relation kvs#)))))

(defn make-seq-selector* [relation keys]
  (let [relation (canonicalize-relation relation)
        selector (selector-symbol relation)]
    `(do
       (defmacro ~selector [& kvs#]
         (generate-seq-run-form '~relation kvs# '~keys))
       (defmacro ~(one-selector-symbol relation) [& kvs#]
         (generate-one-seq-form '~relation kvs# '~keys)))))

    
