(ns peano.t-fiddle
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        [clojure.set :only [difference]]
        clojure.pprint
        peano.sweet))

(data [animal :by :name]
      {:name "betty" :species :bovine}
      {:name "hank" :species :equine})

(data [procedure :by :name]
      {:name "hoof trim", :species-rule :equine-only}
      {:name "superovulation", :species-rule :bovine-only}
      {:name "exam", :species-rule :all}
      )

(defn procedure-species?? [procedure species]
  (l/fresh [rule]
    (procedure-species-rule?? procedure rule)
    (l/conde ( (l/== species :equine) (l/== rule :equine-only))
             ( (l/== species :bovine) (l/== rule :bovine-only))
             ( (l/== rule :all) ))))

(fact "can find procedure species pair"
  (l/run* [q] (procedure-species?? "superovulation" q)) => [:bovine]
  (l/run* [p] (procedure-species?? p :equine)) => (just "hoof trim" "exam") :in-any-order
  (l/run* [q]
    (l/fresh [p s]
      (procedure-species?? p s)
      (l/== [p s] q)))
  => (just ["hoof trim" :equine] ["exam" '_.0] ["superovulation" :bovine] :in-any-order))



;; (defn procedure-applies-to-animal-o [procedure animal]
;;   (l/fresh [species]
;;      (animal-species-o animal species)
;;      (procedure-species-o procedure species)))

;; (defn procedure-applies-to-animal??*
;;   [desired-count &
;;    {:keys [procedure animal]
;;     :or {procedure (l/lvar)
;;          animal (l/lvar)} 
;;     :as args}]
;;   (let [desired-keys  (remove #(contains? (set (keys args)) %) [:animal :procedure])
;;         qvar (gensym "q-")
;;         gensyms {:animal (gensym "animal-"), :procedure (gensym "procedure-")} 
;;         setter (case (count desired-keys)
;;                      0 `(l/== true ~qvar)
;;                      1 `(l/== ~(gensyms (first desired-keys)) ~qvar)
;;                        `(l/== (list ~(gensyms :procedure)
;;                                     ~(gensyms :animal)) ~qvar))
;;         runner (cond (zero? (count desired-keys))  '(l/run 1)
;;                      (= :all desired-count)        '(l/run*)
;;                      :else                         `(l/run ~desired-count))

;;         core-calculation 
;;         `(~@runner [~qvar]
;;                  (l/fresh [~(gensyms :procedure) ~(gensyms :animal)]
;;                           (procedure-applies-to-animal-o ~(gensyms :procedure)
;;                                                          ~(gensyms :animal))
;;                           (l/== ~(gensyms :procedure) ~procedure)
;;                           (l/== ~(gensyms :animal) ~animal)
;;                           ~setter))]
;;     (if (zero? (count desired-keys))
;;       `(not (empty? ~core-calculation))
;;       core-calculation)))

;; (defmacro procedure-applies-to-animal?? [& args]
;;   ( (partial apply procedure-applies-to-animal??*)
;;     (if (number? (first args)) args (cons :all args))))

;; (defmacro first-procedure-applies-to-animal?? [& args]
;;   `(first ~( (partial apply procedure-applies-to-animal??* 1) args)))

;; (def full-hank-or-full-betty (chatty-checker [actual]
;;    (or (= ["superovulation" "betty"] actual)
;;        (= ["hoof trim" "hank"] actual))))

;; (fact "trying to match everything"
;;   (procedure-applies-to-animal??) => (just ["superovulation" "betty"]
;;                                            ["hoof trim" "hank"]
;;                                            ["exam" "betty"]
;;                                            ["exam" "hank"]
;;                                            :in-any-order)
;;   (first (procedure-applies-to-animal?? 1)) => full-hank-or-full-betty
;;   (first-procedure-applies-to-animal??) => full-hank-or-full-betty)


;; (def hank-or-betty (chatty-checker [actual]
;;    (or (= "betty" actual)
;;        (= "hank" actual))))

;; (fact "leaving only one relation unspecified "
;;   (procedure-applies-to-animal?? :animal "hank") => (just "hoof trim" "exam" :in-any-order)
;;   (procedure-applies-to-animal?? 1 :procedure "superovulation") => #(hank-or-betty (first %))
;;   (first-procedure-applies-to-animal?? :procedure "superovulation") => hank-or-betty)

;; (fact "specifying all relations"
;;   (procedure-applies-to-animal?? :animal "hank"  :procedure "superovulation") => falsey
;;   (procedure-applies-to-animal?? :animal "hank"  :procedure "hoof trim") => truthy)



