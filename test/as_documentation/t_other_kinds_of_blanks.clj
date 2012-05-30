(ns as-documentation.t-other-kinds-of-blanks
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        [as-documentation.t-blank-filling
         :only [processor simplify-and-process
                postprocessor make-and-install-new-lvar-with-properties]]
        [peano.sweet :only [suggested-classifier make-forest-selector]]
        [peano.guidance :only [with-lvar assoc-into-vector]]
        clojure.pprint))

;;; This file demonstrates two other kinds of "blanks". A string
;;; requires a particular animal (named by the string):
;;;    ["hank" -]
;;;
;;; A symbol other than - says that symbol should be used as a logic
;;; variable, rather than having one be generated. The idea is that
;;; you'll be able to pass in new logic clauses at macro-call-time to
;;; further constrain the output. This isn't implemented, though.



;;; We have to explicitly `use` the logic-functions defined in the
;;; other file, even though they don't appear in this namespace's
;;; literal text. (They do appear in the macroexpansion.)
(future-fact "Logic functions are namespace-qualified")
(use '[as-documentation.t-blank-filling
       :only [animal-name?? animal-species??
              procedure-name?? procedure-species??]])


        
(defmethod processor :presupplied-lvar [guidance lvar lvar-type]
  (vector (-> guidance
              (with-lvar lvar)
              (assoc-into-vector lvar-type lvar))
          lvar))

(defmethod processor :blank-that-identifies [guidance identifier lvar-type]
  (make-and-install-new-lvar-with-properties guidance lvar-type {:name identifier}))           

;;; These two blanks, plus the earlier two, are the sort of "default",
;;; so there's a default classificer:
(def guidance {:classifier suggested-classifier
               :processor simplify-and-process
               :postprocessor postprocessor})

(make-forest-selector reservation guidance)


;; (println "============== Named animal")
;; (pprint (reservation?> ["hank" -] [{:species :bovine} -]))

;; (println "============== Specific lvars")
;; (pprint (reservation?> [a b] [c d]))


;;; Tests

(use '[peano.guidance :only [property-narrower]])
(use '[peano.guidance :only [property-narrower]])

(fact "a named lvar is used instead of a generated one"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  'my-procedure
                                                  ..irrelevant.. 1)]
    lvar => 'my-procedure
    new-guidance => (contains {:lvars-needed [lvar], :procedure [lvar]})))
     

(fact "a string constrains the name"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  "betsy"
                                                  ..irrelevant.. 0)
        narrower (property-narrower [:animal :name] lvar "betsy")]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [narrower]})))
  
  


(fact "named lvars are at the moment observably only the same as dashes" 
  (let [result (reservation?> [a b] [c d])]
    result => (contains [[["julie" "superovulation"] ["hank" "hoof trim"] ]])))
  
(fact "more complex ones" 
  (let [result (reservation?> ["hank" -] [{:species :bovine} -])]
    (count result) => 4
    result => (just [[["hank" "casting teeth"] ["betty" "superovulation"]]
                     [["hank" "hoof trim"] ["betty" "superovulation"]]
                     [["hank" "casting teeth"] ["julie" "superovulation"]]
                     [["hank" "hoof trim"] ["julie" "superovulation"]]]
                    :in-any-order)))
