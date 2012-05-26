(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))


(defn generate-run-form
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
       (generate-run-form base-relation (rest kvs) (first kvs))
       (generate-run-form base-relation kvs false))))

(defn generate-one-form [base-relation kvs]
  `(first ~(generate-run-form base-relation kvs 1)))

(defn make-did-selector* [base-relation]
  (let [selector (selector-symbol base-relation)]
    `(do
       (defmacro ~selector [& kvs#]
         (generate-run-form '~base-relation kvs#))
       (defmacro ~(one-selector-symbol base-relation) [& kvs#]
         (generate-one-form '~base-relation kvs#)))))
