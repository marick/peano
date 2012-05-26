(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))


(defn generate-did-run-form
  ([base-relation kvs run-count]
     (let [q (gensym "q")
           pairs (if (map? (first kvs))
                   (into [] (first kvs))
                   (partition 2 kvs))
           selector-for-did `(~(query-symbol base-relation) ~q)
           narrowing-selectors (map (fn [[key value]]
                                      `( ~(query-symbol base-relation key) ~q ~value))
                                    pairs)]
       `(l/run ~run-count [~q] ~selector-for-did ~@narrowing-selectors)))
  ([base-relation kvs]
     (if (number? (first kvs))
       (generate-did-run-form base-relation (rest kvs) (first kvs))
       (generate-did-run-form base-relation kvs false))))

(defn generate-one-did-form [base-relation kvs]
  `(first ~(generate-did-run-form base-relation kvs 1)))

(defn make-did-selector* [base-relation]
  (let [selector (selector-symbol base-relation)]
    `(do
       (defmacro ~selector [& kvs#]
         (generate-did-run-form '~base-relation kvs#))
       (defmacro ~(one-selector-symbol base-relation) [& kvs#]
         (generate-one-did-form '~base-relation kvs#)))))



(defn generate-seq-run-form
  ([relation keys kvs run-count]
     (let [q (gensym "q")
           pairs (if (map? (first kvs))
                   (into [] (first kvs))
                   (partition 2 kvs))
           selector-for-result `(l/== [~@(keys-to-lvars keys)] ~q)
           relation-query `(~(query-symbol relation) ~@(keys-to-lvars keys))
           narrowing-selectors (map (fn [[key value]]
                                      `(l/== ~(key-to-lvar key) ~value))
                                    pairs)]
       `(l/run ~run-count [~q]
               (l/fresh [~@(keys-to-lvars keys)]
                        ~selector-for-result ~relation-query ~@narrowing-selectors))))
  ([relation keys kvs]
     (if (number? (first kvs))
       (generate-seq-run-form relation keys (rest kvs) (first kvs))
       (generate-seq-run-form relation keys kvs false))))

(defn generate-one-seq-form [relation keys kvs]
  `(first ~(generate-seq-run-form relation keys kvs 1)))

(defn make-seq-selector* [relation keys]
  (let [relation (canonicalize-relation relation)
        selector (selector-symbol relation)]
    `(do
       (defmacro ~selector [& kvs#]
         (generate-seq-run-form '~relation '~keys kvs#))
       (defmacro ~(one-selector-symbol relation) [& kvs#]
         (generate-one-seq-form '~relation '~keys kvs#)))))

    
