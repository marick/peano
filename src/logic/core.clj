(ns logic.core
  (:require [clojure.core.logic :as l])
  (:use [logic.relations :only [data*]]))

(defmacro data [data-type & data-maps]
  (data* data-type data-maps))
  
  
