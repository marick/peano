(ns peano.t-blank-filling
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.sweet
        peano.blank-filling))

(unfinished classifier work-with-blank postprocessor)

(def guidance {:classifier #'classifier
               :processor #'work-with-blank
               :postprocessor #'postprocessor})

(fact "trivial case"
  (fill-in-the-blanks guidance '[]) => ...final-result...
  (provided
    (classifier anything) => irrelevant :times 0 
    (work-with-blank guidance ...blank... ...levels-above... ...count-to-left...) => irrelevant :times 0
    (postprocessor guidance anything) => ...final-result...))
  

(fact "no blanks to fill"
  (fill-in-the-blanks guidance '[[:m]]) => ...final-result...
  (provided
    (classifier :m) => nil
    (work-with-blank guidance ...blank... ...levels-above... ...count-to-left...) => irrelevant :times 0
    (postprocessor guidance '[[:m]]) => ...final-result...))
  

(fact "a blank"
  (fill-in-the-blanks guidance '[_]) => ...final-result...
  (provided
    (classifier '_) => :not-nil
    (work-with-blank guidance '_ 0 0) => [(assoc guidance :some :change) 'lvar]
    (postprocessor (contains {:some :change}) '[lvar]) => ...final-result...))


(fact "locations are reported correctly"
  (fill-in-the-blanks guidance '[[_ [_ _]]]) => ...final-result...
  (provided
    (classifier '_) => :not-nil
    (work-with-blank anything '_ 1 0) => [guidance 'lvar1]
    (work-with-blank anything '_ 2 0) => [guidance 'lvar1]
    (work-with-blank anything '_ 2 1) => [guidance 'lvar1]
    (postprocessor anything anything) => ...final-result...))


(fact "there is a suggested way of classifying leaf nodes"
  (suggested-classifier '-) => :unconstrained-blank
  (suggested-classifier 'other-symbol) => :presupplied-lvar
  (suggested-classifier "string") => :blank-that-identifies
  (suggested-classifier {:species :bovine}) => :blank-with-properties
  (suggested-classifier '(something else)) => nil)




(comment
  
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

)
