(ns logic.relations
  (:require [clojure.core.logic :as l]))

(defn relation-symbol [prefix-symbol suffix-key]
  (symbol (str (name prefix-symbol) "-" (name suffix-key) "-o")))

(defn typelike-symbol [key]
  ;; Why can I not create a symbol from a key?
  key)

(defn index-relation [relation-symbol field-name]
  `(l/defrel ~relation-symbol ~field-name))

(defn index-facts [relation-name values]
  (map (fn [value] `(l/fact ~relation-name ~value))
       values))

(defn binary-relation [relation-symbol index-name field-name]
  `(l/defrel ~relation-symbol ~index-name ~field-name))

(defn binary-facts [relation-name index-values field-values]
  (map (fn [index-value field-value]
         `(l/fact ~relation-name ~index-value ~field-value))
       index-values
       field-values))

(defn index-do-form [prefix index-key instances]
  (let [relation-symbol (relation-symbol prefix index-key)
        relation-index-name (typelike-symbol index-key)]
    `(do ~(index-relation relation-symbol relation-index-name)
         ~@(index-facts relation-symbol (map index-key instances)))))

(defn other-do-form [prefix index-key other-key instances]
  (let [relation-symbol (relation-symbol prefix other-key)
        relation-index-name (typelike-symbol index-key)
        relation-argname (typelike-symbol other-key)]
    `(do ~(binary-relation relation-symbol relation-index-name relation-argname)
         ~@(binary-facts relation-symbol
                         (map index-key instances)
                         (map other-key instances)))))

(defn data-accessor [prefix index-key instances]
  `(defn ~(symbol (str (name prefix) "-data")) [index-value#]
     (first (filter #(= index-value# (get % ~index-key))
                    (vector ~@instances)))))

(defn data* [index-description instances]
  (let [prefix (first index-description)
        index-key (last index-description)
        other-keys (keys (dissoc (first instances) index-key))
        index-do-form (index-do-form prefix index-key instances)
        remaining-forms (map (fn  [other-key]
                               (other-do-form prefix index-key other-key instances))
                             other-keys)]
    `(do ~index-do-form
         ~@remaining-forms
         ~(data-accessor prefix index-key instances))))
