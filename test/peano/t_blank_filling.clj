(ns peano.t-blank-filling
  (:require [clojure.core.logic :as l])
  (:use midje.sweet
        clojure.pprint
        peano.blank-filling))

(unfinished g)
(defn f [n] (g {:a 'complicated :f odd?} n))

(fact
  (f 1) => 1
  (provided
    (g anything 1) => 1))

(unfinished classification work-with-blank postprocessing)

(def guidance {:classification #'classification
               :process-blank #'work-with-blank
               :postprocessing #'postprocessing})

(fact "trivial case"
  (fill-in-the-blanks guidance '[]) => irrelevant
  (provided
    (classification anything) => irrelevant :times 0 
    (work-with-blank guidance ...blank... ...levels-above... ...count-to-left...) => irrelevant :times 0
    (postprocessing guidance anything) => anything))
  

(fact "no blanks to fill"
  (fill-in-the-blanks guidance '[[:m]]) => irrelevant
  (provided
    (classification :mm) => nil
    (work-with-blank guidance ...blank... ...levels-above... ...count-to-left...) => irrelevant :times 0
    (postprocessing guidance '[[:m]]) => anything))
  



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
