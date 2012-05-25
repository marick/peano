(ns peano.core
  (:require [clojure.core.logic :as l])
  (:use [peano.data :only [data*]])
  (:use [peano.selectors :only [make-selector-functions*]]))


(defmacro data [data-type & data-maps]
  (data* data-type data-maps))

(defmacro make-selector-functions [relation did]
  (make-selector-functions* relation did))
  
