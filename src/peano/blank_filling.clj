(ns peano.blank-filling
  (:require [clojure.zip :as zip])
  (:use peano.tokens))

(defn fill-in-the-zipper [guidance loc]
  (cond (zip/end? loc)
        ( (:postprocessor guidance) guidance (zip/root loc))

        (zip/branch? loc)
        (recur guidance (zip/next loc))

        ( (:classifier guidance) (zip/node loc))
        (let [[guidance lvar] ( (:processor guidance)
                                guidance (zip/node loc)
                                (dec (count (zip/path loc))) (count (zip/lefts loc)))]
          (recur guidance
                 (zip/next (zip/replace loc lvar))))

        :else
        (recur guidance (zip/next loc))))

(defn suggested-classifier [form]
  (cond (= '- form) :unconstrained-blank
        (string? form) :blank-that-identifies
        (symbol? form) :presupplied-lvar
        (map? form) :blank-with-properties))

(defn generate-run-form [run-count guidance tree]
  (let [q (gensym "q")]
    `(l/run ~run-count [~q]
            (l/fresh [~@(guidance :lvars-needed)]
                     (l/== ~tree ~q)
                     ~@(guidance :narrowers)))))


