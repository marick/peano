(ns peano.t-reservation-example
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.core
        peano.guidance
        [peano.blank-filling :only [suggested-classifier]]))

(defmulti processor
  (fn [guidance blank & _] ((:classifier guidance) blank)))

(defn make-and-install-new-lvar [guidance lvar-type]
  (let [[guidance lvar] (with-new-lvar guidance lvar-type)
        guidance (assoc-into-vector guidance lvar-type lvar)]
    [guidance lvar]))

;; TODO: This is really asking for something state-monad-like
(defmethod processor :unconstrained-blank [guidance _ lvar-type]
  (make-and-install-new-lvar guidance lvar-type))

(defmethod processor :presupplied-lvar [guidance lvar lvar-type]
  (vector (-> guidance
              (with-lvar lvar)
              (assoc-into-vector lvar-type lvar))
          lvar))

(defn make-and-install-new-lvar-with-properties [guidance lvar-type property-map]
   (let [[guidance lvar] (make-and-install-new-lvar guidance lvar-type)
         guidance (reduce (fn [guidance [key value]]
                            (with-narrower guidance (property-narrower [lvar-type key]
                                                                       lvar value)))
                          guidance
                          property-map)]
     [guidance lvar]))
  

(defmethod processor :blank-with-properties [guidance property-map lvar-type]
   (make-and-install-new-lvar-with-properties guidance lvar-type property-map))

(defmethod processor :blank-that-identifies [guidance identifier lvar-type]
  (make-and-install-new-lvar-with-properties guidance lvar-type {:name identifier}))           

(defn simplify-and-process [guidance blank _ count-to-left]
  (processor guidance blank
                   (if (= 0 count-to-left) :procedure :animal)))

(def guidance {:classifier suggested-classifier
               :processor simplify-and-process
               :postprocessor (fn [x y] [x y])})

;;; About processing

;;; Note: I'm avoiding prerequisites to make these tests easier to read for the
;;; not-Midje-initiated.

(fact "an unqualified first-position blank adds a procedure lvar"
  (let [[new-guidance lvar] (simplify-and-process guidance '- ..irrelevant.. 0)]
    lvar => 'procedure-0
    new-guidance => (contains {:lvars-needed [lvar], :procedure [lvar]})))
     
(fact "an unqualified second-position blank adds an animal lvar"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  '-
                                                  ..irrelevant.. 1)]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]})))
     

(fact "a named lvar is used instead of a generated one"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  'my-procedure
                                                  ..irrelevant.. 0)]
    lvar => 'my-procedure
    new-guidance => (contains {:lvars-needed [lvar], :procedure [lvar]})))
     

(fact "a property map generates an lvar and constrains it"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  {:prop 'val :and 'val2}
                                                  ..irrelevant.. 1)
        prop-narrower (property-narrower [:animal :prop] lvar 'val)
        and-narrower (property-narrower [:animal :and] lvar 'val2)]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [prop-narrower and-narrower]})))

(fact "a string constrains the name"
  (let [[new-guidance lvar] (simplify-and-process guidance
                                                  "betsy"
                                                  ..irrelevant.. 1)
        narrower (property-narrower [:animal :name] lvar "betsy")]
    lvar => 'animal-0
    new-guidance => (contains {:lvars-needed [lvar], :animal [lvar]
                               :narrowers [narrower]})))
  
  
(prn (fill-in-the-blanks guidance '[[_ "hank"] [myproc {:legs 4}]]))
