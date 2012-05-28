(ns peano.blank-filling
  (:require [clojure.core.logic :as l]
            [clojure.zip :as zip])
  (:use peano.tokens))

(defn fill-in-the-blanks [guidance vector-structure]
  (loop [guidance guidance
         loc (zip/vector-zip vector-structure)]
    (cond (zip/end? loc)
          ( (:postprocessing guidance) guidance (zip/root loc))

          (zip/branch? loc)
          (recur guidance (zip/next loc))

          ( (:classification guidance) (zip/node loc))
          (let [[guidance lvar] ( (:process-blank guidance)
                                  guidance (zip/node loc)
                                  (dec (count (zip/path loc))) (count (zip/lefts loc)))]
            (recur guidance
                   (zip/next (zip/replace loc lvar))))

          :else
          (recur guidance (zip/next loc)))))

(comment 

(defn fill-in-one-blank [accumulator vertical-position horizontal-position]
  (let [replacements-for-position ((accumulator :counts) horizontal-position)
        replacement (str ((accumulator :names) horizontal-position)
                         "-"
                         replacements-for-position)]
    (vector (assoc-in accumulator [:counts horizontal-position] (inc replacements-for-position))
            replacement)))

(defn predefined-logic-var [accumulator logic-var]
  (merge-with conj accumulator {:logic-vars-needed logic-var}))


(defn fill-in-the-blanks-1 [loc accumulator]
  (cond (zip/end? loc)
        (assoc (select-keys accumulator [:extra-restrictions :logic-vars-needed])
          :filled-in (zip/root loc))

        (= (zip/node loc) '-)
        (let [[accumulator replacement]
              (fill-in-one-blank accumulator
                                 (dec (count (zip/path loc)))
                                 (count (zip/lefts loc)))]
          (recur (zip/next (zip/replace loc replacement))
                 (merge-with conj accumulator {:logic-vars-needed replacement})))

        (symbol? (zip/node loc))
        (recur (zip/next loc)
               (predefined-logic-var accumulator (zip/node loc)))

        (string? (zip/node loc))
        (let [[accumulator replacement]
              (fill-in-one-blank accumulator
                                 (dec (count (zip/path loc)))
                                 (count (zip/lefts loc)))]
          (recur (zip/next (zip/replace loc replacement))
                 (merge-with conj accumulator {:logic-vars-needed replacement
                                               :extra-restrictions (zip/node loc)})))

        :else
        (recur (zip/next loc) accumulator)))
  

(defn fill-in-the-blanks [nested-items guidance]
  (fill-in-the-blanks-1 (zip/vector-zip nested-items)
                        (assoc guidance
                               :extra-restrictions []
                               :logic-vars-needed [])))   


(def guidance {:names ["animal" "procedure"]
               :counts [0 0]}) 

(defn build-run* [reservation ab-pair extra-clauses]
  (let [body `( (l/== (cons :reservation ~reservation) ~'q)
                (permitted?? ~@ab-pair)
                ~@extra-clauses)]
    `(l/run false [~'q] (l/fresh ~ab-pair ~@body))))

;; (make-reservation [:reservation [a b]]) 
;; (make-reservation [:reservation [a b]] (l/== a b)) 

(defmacro make-reservation [reservation & manual-extra-clauses]
  (let [constructed-reservation reservation
        logic-vars '[a b]
        extra-clauses-from-reservation '[(l/== a "hank")]]
    (build-run* constructed-reservation logic-vars
                (concat manual-extra-clauses extra-clauses-from-reservation))))

)

