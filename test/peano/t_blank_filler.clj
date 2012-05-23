(ns peano.t-blank-filler
  (:require [clojure.core.logic :as l]
            [clojure.zip :as zip])
  (:use midje.sweet
        [clojure.set :only [difference]]
        clojure.pprint
        peano.core))

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

(fact "trivial case produces no change"
  (fill-in-the-blanks '[] guidance)
  => (contains {:filled-in []}))

(fact "dashes describe blanks to be filled in"
  (fill-in-the-blanks '[-] guidance)
  => (contains {:filled-in ["animal-0"], :logic-vars-needed ["animal-0"]})

  (fill-in-the-blanks '[[- -] [- -]] guidance)
  => (contains {:filled-in [["animal-0" "procedure-0"] ["animal-1" "procedure-1"]],
                :logic-vars-needed ["animal-0" "procedure-0" "animal-1" "procedure-1"]}))


(fact "logical variables can be specified"
  (fill-in-the-blanks '[- b] guidance)
  => (contains {:filled-in '["animal-0" b]
                :logic-vars-needed '["animal-0" b]}))

(fact "strings name the 'did' of a generated variable, thus restricting it"
  (fill-in-the-blanks '["hank" b] guidance)
  => (contains {:filled-in '["animal-0" b]
                :logic-vars-needed '["animal-0" b]
                :extra-restrictions '["hank"]}))
