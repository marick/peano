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

(defn as-date [s]
  (LocalDate/parse s))

(defn as-date-string [local-date]
  (str local-date))

(fact "as-date and as-date-string are inverses"
  (as-date-string (as-date "2011-01-03")) => "2011-01-03")

(defn non-decreasing?
  "True iff x and y are Comparable and x<=y"
  [supposedly-earlier supposedly-not-earlier]
  (<= (.compareTo supposedly-earlier supposedly-not-earlier) 0))

(fact "date ranges shouldn't decrease"
  (let [date1 (as-date "2001-10-11")
        date1-also (as-date "2001-10-11")
        date2 (as-date "2001-10-12")]
    (non-decreasing? date1 date1-also) => truthy
    (non-decreasing? date1 date2) => truthy
    (non-decreasing? date2 date1) => falsey))


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

;;; This file defines three selectors. The first defines selectors reminiscent of
;;; ones you've already seen:
;;;
;;; (timeslice?>)                               ; all timeslices
;;; (timeslice?> "2012-01-02")                  ; all timeslices with this first date
;;; (timeslice?> "2012-01-02" "2012-01-03")     ; first and end date
;;; (timeslice?> "001")                         ; all timeslices with evening time-of-day
;;; (timeslice?> 3)                             ; only three timeslices
;;; (one-timeslice "2012-01-02" "001")          ; only one timeslice, not in a sequence

;;; The general implementation strategy looks like this:
;;;
;;; To get all possible first/last/time-of-day combinations, I'd use this form:
;;;
;;;  (l/run* [result]
;;;         (l/fresh [first last tod]
;;;                  (l/== [first last tod] result)
;;;                  (date?? first)
;;;                  (date?? last)
;;;                  (time-of-day?? tod)))
;;;
;;; This would produce timeslices with a "last" date before the "first". To avoid that,
;;; I add in a pseudo derived relation:
;;;
;;;                  (non-decreasing-dates??- first last)
;;;
;;;
;;; For each of `first`, `last`, and `tod`, the value may be passed in or looked up. 
;;; If looked up, the `date??` and `time-of-day??` relations are used, as shown above.
;;; If a value was passed in, the clause is replaced with something like this:
;;;
;;;                 (l/== first "2012-03-08")
;;; 
;;; This presents a difficulty, though: the "shape" of the `run` form
;;; changes depending what arguments the user gives. That means we're
;;; in the world of code-writing code / macros, not in the world of
;;; functions. There's a teensy problem with that. We'd naturally want
;;; to write this:
;;;
;;;     (l/run* [result#]
;;;            (l/fresh [first# last# tod#]
;;;                     (l/== [first# last# tod#] result#)
;;;                     (date?? first#)
;;;                     (date?? last#)
;;;                     (time-of-day?? tod#)))
;;;                     
;;; But `run` is a macro too, with the result that it will appear to
;;; the Clojure reader that we're using un-#-ified variables in `let`
;;; and function arglists. So we have to return to the old Lisp days of
;;; creating our own symbols with `gensym` and substituting those in:
;;;
;;;      (let [timeslice-sym (gensym "timeslice-")
;;;            first-sym (gensym "first-")
;;;            last-sym (gensym "last-")
;;;            tod-sym (gensym "time-of-day-")]
;;;        `(l/run* [~timeslice-sym]
;;;              (l/fresh [~first-sym ~last-sym ~tod-sym]
;;;                       (l/== [~first-sym ~last-sym ~tod-sym] ~timeslice-sym)
;;;                       ...

;;; Details

;;; By reaching into the underpinnings of core.logic, you can perform
;;; arbitrary calculations on the values of logic variables and, from
;;; the results, either succeed or fail. Functions that do this can
;;; look like ordinary relations, but they do not associate variables
;;; with values: the association has to be done before the function is
;;; called. This breaks the illusion that logical deduction is
;;; unordered.

;;; I distinguish such functions with a trailing minus sign: they're
;;; lesser than other derived relations.

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
                                (l/== last "2012-01-02")
                                (non-decreasing-dates??- first last)))]
    result => (just [["2012-01-01" "2012-01-02"] ["2012-01-02" "2012-01-02"]]
                    :in-any-order)))

;;; core.logic defines two run functions: one that returns all success values (run*) and
;;; one that takes a count (run N). Behind the scenes, `run*` is just defined as `run false`.

(def generate-all false) 


(defn deduce-desires
  "Interpret a sequence of args and make it into a map"
  [args]
  (reduce (fn [accumulator arg]
            ( #(assoc accumulator % arg)
              (cond (number? arg) :run-count
                    (time-of-day? arg) :time-of-day
                    (date? arg) (if (:first accumulator) :last :first)
                    :else (throw (Error. (str "Unknown argument: " arg))))))
          {:run-count generate-all}
          args))

(fact "The argument list is flexible"
  (deduce-desires ["2012-12-31"])
  => {:first "2012-12-31" :run-count generate-all}
  (deduce-desires ["2012-01-01" "2012-12-31"])
  => {:first "2012-01-01" :last "2012-12-31" :run-count generate-all}
  (deduce-desires ["011"])
  => {:time-of-day "011" :run-count generate-all}
  (deduce-desires ["100" "2012-12-31"])
  => {:first "2012-12-31":time-of-day "100" :run-count generate-all}
  (deduce-desires [1 "100" "2012-12-31"])
  => {:first "2012-12-31":time-of-day "100" :run-count 1}
  (deduce-desires ["0"]) => (throws Error))


(defn choose-narrower [type-selector lvar value]
  (if value
    `(l/== ~lvar ~value)      ; use user's value.
    `(~type-selector ~lvar))) ; find value.

(defn generate-timeslice-selector-run-form [desires]
  (let [timeslice-sym (gensym "timeslice-")
        first-sym (gensym "first-")
        last-sym (gensym "last-")
        tod-sym (gensym "time-of-day-")]
    `(l/run ~(:run-count desires) [~timeslice-sym]
          (l/fresh [~first-sym ~last-sym ~tod-sym]
                   (l/== [~first-sym ~last-sym ~tod-sym] ~timeslice-sym)
                   ~(choose-narrower 'date?? first-sym (:first desires))
                   ~(choose-narrower 'date?? last-sym  (:last desires))
                   ~(choose-narrower 'time-of-day?? tod-sym (:time-of-day desires))
                   (non-decreasing-dates??- ~first-sym ~last-sym)))))

(defn generate-one-timeslice-selector [desires]
  `(first ~(generate-timeslice-selector-run-form (assoc desires :run-count 1))))

(defmacro make-timeslice-selectors []
  `(do
     (defmacro timeslice?> [& args#]
       (generate-timeslice-selector-run-form (deduce-desires args#)))
     (defmacro one-timeslice?> [& args#]
       (generate-one-timeslice-selector (deduce-desires args#)))))

(make-timeslice-selectors)


(defn date-only [[first last _]] [first last])
(fact "Can select all non-descending timeslices"
  (let [actual (timeslice?>)]
    actual => (contains [["2012-01-01" "2012-01-01" "001"]])
    (count (filter #(= (date-only %) ["2012-01-01" "2012-12-31"]) actual))
    => 7
    (set (map #(apply non-decreasing? (date-only %)) actual))
    => #{true}))

(fact "Can filter by time of day"
  (let [actual (timeslice?> "001")]
    actual => (contains [["2012-01-01" "2012-01-01" "001"]])
    actual =not=> (contains [["2012-01-01" "2012-01-01" "111"]])))

(fact "Can filter by start date"
  (let [actual (timeslice?> "2012-01-02")]
    (set (map first actual)) => #{"2012-01-02"}
    (count actual) => (+ 7 7 7))
  (count (timeslice?> "2012-12-31")) => 7)

(fact "Can filter by start and end date"
  (let [actual (timeslice?> "2012-01-02" "2012-01-03")]
    (set (map date-only actual)) => #{["2012-01-02" "2012-01-03"]}
    (count actual) => 7))

(fact "Can limit the number returned"
  (let [actual (timeslice?> 3 "2012-01-02")]
    (set (map first actual)) => #{"2012-01-02"}
    (count actual) => 3))

(fact "Has a 'one' version"
  (let [actual (one-timeslice?> 3 "2012-01-02" "2012-01-03")]
    (date-only actual) => ["2012-01-02" "2012-01-03"]))

;;; The Other Function

;;; In this particular application, I often needed test cases like a
;;; timeslice like "the morning of two consecutive days". That could
;;; look like this:
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
                                           ~(choose-narrower 'date?? first-sym first-val)
                                           ~(choose-narrower 'time-of-day?? tod-sym tod-val)
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
