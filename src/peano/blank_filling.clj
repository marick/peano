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

(defn generate-run-form [run-count guidance tree]
  (let [q (gensym "q")]
    `(l/run ~run-count [~q]
            (l/fresh [~@(guidance :lvars-needed)]
                     (l/== ~tree ~q)
                     ~@(guidance :narrowers)))))


(defn generate-forest-filler-run-form
  ([run-count guidance tree]
     (apply (partial generate-run-form run-count)
            (fill-in-the-zipper guidance (clojure.zip/vector-zip (vec tree)))))
  ([guidance tree]
     (if (number? (first tree))
       (generate-forest-filler-run-form (first tree) guidance (rest tree))
       (generate-forest-filler-run-form false        guidance tree ))))

(defn generate-one-forest-filler-form [guidance tree]
  `(first ~(generate-forest-filler-run-form 1 guidance tree)))
  
(defn def-forest-fillers* [basename guidance]
  (let [selector (selector-symbol basename)]
    `(do
       (defmacro ~selector [& tree#]
         (generate-forest-filler-run-form ~guidance tree#))
       (defmacro ~(one-selector-symbol basename) [& tree#]
         (generate-one-forest-filler-form ~guidance tree#)))))

