(ns peano.t-fiddle
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        [clojure.set :only [difference]]
        clojure.pprint
        peano.core))

(data [animal :by :name]
      {:name "betty" :species :bovine}
      {:name "hank" :species :equine})

(data [procedure :by :name]
      {:name "hoof trim", :species :equine}
      {:name "superovulation", :species :bovine})

(defn procedure-applies-to-animal-o [procedure animal]
  (l/fresh [species]
     (animal-species-o animal species)
     (procedure-species-o procedure species)))


(defmacro procedure-applies-to-animal-none    []
  `(l/run* [q#] (l/fresh [animal# procedure#]
                       (procedure-applies-to-animal-o procedure# animal#)
                       (l/== [animal# procedure#] q#))))

(procedure-applies-to-animal-none)

(defmacro procedure-applies-to-animal-animal [animal]
  `(l/run* [q#] (l/fresh [animal# procedure#]
                     (procedure-applies-to-animal-o procedure# animal#)
                     (l/== animal# ~animal)
                     (l/== procedure# q#))))

(procedure-applies-to-animal-animal "betty")


(defmacro procedure-applies-to-animal-both [procedure animal]
  `(not (empty? (l/run* [q#] (l/fresh [animal# procedure#]
                     (procedure-applies-to-animal-o procedure# animal#)
                     (l/== ~animal animal#)
                     (l/== ~procedure procedure#))))))

(defn procedure-applies-to-animal??*
  [desired-count &
   {:keys [procedure animal]
    :or {procedure (l/lvar)
         animal (l/lvar)} 
    :as args}]
  (let [desired-keys  (remove #(contains? (set (keys args)) %) [:animal :procedure])
        qvar (gensym "q-")
        gensyms {:animal (gensym "animal-"), :procedure (gensym "procedure-")} 
        setter (case (count desired-keys)
                     0 `(l/== true ~qvar)
                     1 `(l/== ~(gensyms (first desired-keys)) ~qvar)
                       `(l/== (list ~(gensyms :procedure)
                                    ~(gensyms :animal)) ~qvar))
        runner (cond (zero? (count desired-keys))  '(l/run 1)
                     (= :all desired-count)        '(l/run*)
                     :else                         `(l/run ~desired-count))

        core-calculation 
        `(~@runner [~qvar]
                 (l/fresh [~(gensyms :procedure) ~(gensyms :animal)]
                          (procedure-applies-to-animal-o ~(gensyms :procedure)
                                                         ~(gensyms :animal))
                          (l/== ~(gensyms :procedure) ~procedure)
                          (l/== ~(gensyms :animal) ~animal)
                          ~setter))]
    (if (zero? (count desired-keys))
      `(not (empty? ~core-calculation))
      core-calculation)))

(defmacro procedure-applies-to-animal?? [& args]
  ( (partial apply procedure-applies-to-animal??*)
    (if (number? (first args)) args (cons :all args))))

(defmacro first-procedure-applies-to-animal?? [& args]
  `(first ~( (partial apply procedure-applies-to-animal??* 1) args)))

