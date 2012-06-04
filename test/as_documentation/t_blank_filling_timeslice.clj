(ns as-documentation.t-blank-filling-timeslice
  (:require [clojure.core.logic :as l]
            clojure.core.logic.arithmetic)
  (:import org.joda.time.LocalDate)
  (:use midje.sweet
        peano.sweet
        clojure.pprint
        [peano.guidance :only [with-new-lvar assoc-into-vector
                               with-narrower property-narrower]]))

;;; This shows how to handle different ways of representing
;;; "timeslices". Consider it an extension of t-blank-filling. In real life,
;;; a reservation has both a hierarchical structure to fill and some
;;; date data to contain. I'm keeping these separate so that the
;;; issues of test generation don't get too bogged down in details of
;;; a particular domain.

;;; A *timeslice* consists of a start date, and end date, and a time of day.
;;; Dates are simple:

(data [date :by :did :selectors]
      {:did "2012-01-01"}
      {:did "2012-01-02"}
      {:did "2012-01-03"}
      {:did "2012-12-31"})

;; Some helper functions:

(defn date? [s]
  (and (string? s)
       (= (count s) 10)))

(defn as-date [s] (LocalDate/parse s))

(defn as-date-string [local-date]
  (str local-date))

(fact "as-date and as-date-string are inverses"
  (as-date-string (as-date "2011-01-03")) => "2011-01-03")



;;; Times of day are a bit more complicated. The day is divided into
;;; morning, afternoon, and evening. A reservation can use any
;;; combination of those, so long as at least one division is used. In
;;; the database, this information is stored as a bitset, and I've
;;; actually found that easiest to use in the tests.

(data [time-of-day :by :did :selectors]
      {:did "100"}  ; morning
      {:did "010"}  ; afternoon
      {:did "001"}  ; evening
      {:did "110"}  ; morning and afternoon
      {:did "101"}
      {:did "011"}
      {:did "111"})  ; all day

(defn time-of-day? [s]
  (and (string? s)
       (= (count s) 3)))


;;; In this particular application, I often needed test cases like a
;;; timeslice like "the morning of two consecutive days". That could look like this:
;;;
;;;   (one-gapped-timeslice?> "2001-01-01" 1 "110")
;;;
;;; or, if I didn't care about the time-of-day:
;;;
;;;   (one-gapped-timeslice?> "2001-01-01" 1 "110")
;;;
;;; or, if I didn't care which consecutive days:
;;;
;;;   (one-gapped-timeslice?> 1 "110")
;;;
;;; I'm only implementing a single-value-returning form (1) in order
;;; to keep this simple and (2) because that's probably the only case
;;; I'd ever want.
;;;
;;; The general implementation strategy looks like this:
;;;
;;; To get all possible first/last/time-of-day combinations, I'd use this form:
;;;
;;;
;;;  (l/run 1 [result]
;;;         (l/fresh [first last tod]
;;;                  (l/== [first last tod] result)
;;;                  (date?? first)
;;;                  (date?? last)
;;;                  (time-of-day?? tod)))
;;;
;;; In this case, we don't want to find the last date - we want to
;;; calculate it from the first date, whether that date was handed in
;;; or looked up, so we wouldn't use the `last` logic variable in the above.
;;; So we have this:
;;;
;;;  (l/run 1 [result]
;;;         (l/fresh [first tod]
;;;                  (l/== [first tod] result)
;;;                  (date?? first)
;;;                  (time-of-day?? tod)))
;;;  ... code that adds the desired gap to `first` to create the `last` value
;;;
;;; Also: we don't want to restrict the `first` value to be one of the
;;; dates in the `date` relation. If the user decides to pick one, she
;;; should get whatever she wants. This can be accomplished by leaving the
;;; `(date?? first)` clause out of the `run` statement:
;;;
;;;  (l/run 1 [result]
;;;         (l/fresh [first tod]
;;;                  (l/== [first tod] result)
;;;                  (time-of-day?? tod))
;;;  ... code that makes the given value argument the `first` date in the return value
;;;  ... code that adds the desired gap to `first` to create the `last` value
;;; 
;;;
;;; The `first` logic variable will be associated with the "any old value" value, which doesn't matter
;;; because we're not going to use its value any way, but rather the one that was passed in.
;;;
;;; This presents a difficulty, though: the "shape" of the `run` form
;;; changes depending what arguments the user gives. That means we're
;;; in the world of code-writing code / macros, not in the world of
;;; functions. There's a teensy problem with that. We'd naturally want
;;; to write this:
;;;
;;;    `(l/run 1 [result#]
;;;        (l/fresh [first# tod#]
;;;                 (l/== [first# tod#] result#)
;;;                 ~@first-date-form
;;;                  (time-of-day?? tod#)))
;;;
;;; But `run` is a macro too, with the result that it will appear to the reader that we're
;;; using un-#-ified variables in let and function arglists. So we have to return to the old days of
;;; creating our own symbols with `gensym` and substituting those in:
;;;
;;;    `(l/run 1 [~result-sym]
;;;        (l/fresh [~first-sym ~tod-sym]
;;;                 (l/== [~first-sym ~tod-sym] ~result-sym)
;;;                 ~@first-date-form
;;;                  (time-of-day?? ~tod-sym)))
;;;



(defmacro one-gapped-timeslice?> [& args]
  (let [first-sym (gensym "first-")
        first-val (first (filter date? args))

        tod-sym (gensym "tod-")
        tod-val (first (filter time-of-day? args))

        result-sym (gensym "timeslice-")
        gap (first (filter number? args))]
    (if-not gap (throw (Error. "Gap argument is required")))
    `(let [results# (first (l/run 1 [~result-sym]
                                  (l/fresh [~first-sym ~tod-sym]
                                           (l/== [~first-sym ~tod-sym] ~result-sym)
                                           ~@(if-not first-val `((date?? ~first-sym)))
                                           (time-of-day?? ~tod-sym))))
           first# (or ~first-val (first results#))
           last# (as-date-string (.plusDays (as-date first#) ~gap))
           tod# (or ~tod-val (second results#))]
       [first# last# tod#])))

(fact "Only the number argument is required"
  (let [[first-date last-date time-of-day] (one-gapped-timeslice?> 0)]
    first-date => date?
    last-date => date?
    time-of-day => time-of-day?
    first-date => last-date))
(fact "The number argument adjusts the last date"
  (let [[first-date last-date time-of-day] (one-gapped-timeslice?> 1)]
    (.minusDays (as-date last-date) 1) => (as-date first-date)))

(fact "The start date can be explicitly given"
  (let [[first-date last-date time-of-day] (one-gapped-timeslice?> 2 "1959-10-23")]
    first-date => "1959-10-23"
    last-date => "1959-10-25"))
    
(fact "The time-of-day can be explicitly given"
  (nth (one-gapped-timeslice?> 0 "110") 2) => "110"
  (nth (one-gapped-timeslice?> 0 "010") 2) => "010"
  (nth (one-gapped-timeslice?> 0 "101") 2) => "101")
    
(fact "And everything can be given"
  (one-gapped-timeslice?> "2011-01-03" "111" 2)
  => ["2011-01-03" "2011-01-05" "111"])




;(prn (macroexpand-1 '(one-gapped-timeslice 1)))
;(prn (one-gapped-timeslice "2011-02-03" 1 "111"))
                 





(defn non-decreasing?
  [supposedly-earlier supposedly-not-earlier]
  (<= (.compareTo supposedly-earlier supposedly-not-earlier) 0))

(fact "date ranges shouldn't decrease"
  (let [date1 (as-date "2001-10-11")
        date1-also (as-date "2001-10-11")
        date2 (as-date "2001-10-12")]
    (non-decreasing? date1 date1-also) => truthy
    (non-decreasing? date1 date2) => truthy
    (non-decreasing? date2 date1) => falsey))





;;; Relation-like clauses These can be used in derived relations
;;; (goals) to make particular combinations of logic variables
;;; fail. They *cannot* be used to associate values with logic
;;; vars. The logic vars must already be bound/associated.

(defn non-decreasing-dates??- [x y]
  (fn [substitutions]
     (let [walked-x (l/walk substitutions x)
           walked-y (l/walk substitutions y)]
       (if (non-decreasing? (as-date walked-x) (as-date walked-y))
         substitutions nil))))

(fact "non-decreasing date pairs can be selected"
  (let [result (l/run* [q]
                       (l/fresh [first last]
                                (l/== [first last] q)
                                (date?? first)
                                (date?? last)
                                (non-decreasing-dates??- first last)))]
    result => (contains [["2012-01-02" "2012-12-31"]])
    result =not=> (contains [["2012-12-31" "2012-01-02"]])))






                           
(comment

  (defn deduce-desires [args]
  (reduce (fn [accumulator arg]
            (cond (number? arg)
                  (assoc accumulator :day-gap arg)

                  (= (count arg) 3)
                  (assoc accumulator :time-of-day arg)

                  (:first accumulator)
                  (assoc accumulator :last arg)

                  :else
                  (assoc accumulator :first arg)))
          {}
          args))

(fact "The argument list is flexible"
  (deduce-desires [5]) => {:day-gap 5}
  (deduce-desires ["2012-12-31"]) => {:first "2012-12-31"}
  (deduce-desires ["2012-01-01" "2012-12-31"]) => {:first "2012-01-01" :last "2012-12-31"}
  (deduce-desires ["011"]) => {:time-of-day "011"}
  (deduce-desires ["100" "2012-12-31" 1]) => {:first "2012-12-31" :day-gap 1 :time-of-day "100"})


;; (defn narrower [which lvar desires]
;;   (if (which desires)
;;     `((l/== ~lvar ~(which desires)))))


;; (defn generate-timeslice-selector-run-form [desires]
;;     (let [timeslicev (gensym "timeslice-")
;;           firstv (gensym "first-")
;;           lastv (gensym "lastv-")
;;           todv (gensym "time-of-day-")
;;           selector-for-result `(l/== [~firstv ~lastv ~todv] ~timeslicev)
;;           type-queries '[ (date?? ~firstv)
;;                           (date?? ~lastv)
;;                           (time-of-day?? ~todv) ]
          

;;     `(l/run* [~timeslicev]
;;           (l/fresh [~firstv ~lastv ~todv]
;;                    (l/== 
;;                    (non-decreasing-dates?? ~firstv ~lastv)
;;                    ~@(narrower :first firstv desires)
;;                    ~@(narrower :last lastv desires)
;;                    ~@(narrower :time-of-day todv desires)))))

  

(defn make-timeslice-macros []
  `(do
     (defmacro timeslice?> [& args#]
       (generate-timeslice-selector-run-form (deduce-desires args#)))
     (defmacro one-timeslice?> [& args#]
       (generate-one-timeslice-selector (deduce-desires args#)))))

;; (defmacro timeslice?> [& args]
          
;; (prn (macroexpand-1 '(timeslice?> "2012-12-31")))
;; (prn (timeslice?> "2012-12-31"))

                                
)
