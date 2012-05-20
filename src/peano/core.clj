(ns peano.core
  (:require [clojure.core.logic :as l])
  (:use [peano.relations :only [data*]]))

(defmacro data [data-type & data-maps]
  (data* data-type data-maps))
  
  
