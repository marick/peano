(ns peano.data
  (:require [clojure.core.logic :as l])
  (:use peano.tokens
        [peano.selectors :only [make-did-selector*]]))

(defn to-make-did-relation [name-in-query-form did-key]
  `(l/defrel ~name-in-query-form ~did-key))

(defn to-add-did-facts [name-in-query-form values]
  (map (fn [value] `(l/fact ~name-in-query-form ~value))
       values))

(defn did-do-form [relation-name did-key instances]
  (let [name-in-query-form (query-symbol relation-name)
        relation-did-name (typelike-symbol did-key)]
    `(do ~(to-make-did-relation name-in-query-form relation-did-name)
         ~@(to-add-did-facts name-in-query-form (map did-key instances)))))


(defn to-make-binary-relation [name-in-query-form did-key field-name]
  `(l/defrel ~name-in-query-form ~did-key ~field-name))
  

(defn to-add-binary-facts [name-in-query-form did-values field-values]
  (map (fn [did-value field-value]
         `(l/fact ~name-in-query-form ~did-value ~field-value))
       did-values
       field-values))

(defn binary-do-form [relation-name did-key other-key instances]
  (let [name-in-query-form (query-symbol relation-name other-key)
        relation-did-name (typelike-symbol did-key)
        relation-argname (typelike-symbol other-key)]
    `(do ~(to-make-binary-relation name-in-query-form relation-did-name relation-argname)
         ~@(to-add-binary-facts name-in-query-form
                             (map did-key instances)
                             (map other-key instances)))))



(defn data-accessor [relation-name did-key instances]
  (let [mapping (group-by did-key instances)]
    `(defn ~(data-symbol relation-name)
       ([did-value#] (first (~mapping did-value#)))
       ([] '~instances))))



(defn data* [did-description instances]
  (let [prefix (first did-description)
        did-key (nth did-description 2)
        all-keys (keys (first instances))
        did-do-form (did-do-form prefix did-key instances)
        selector-definitions (if (> (count did-description) 3)
                               (make-did-selector* prefix))
        remaining-forms (map (fn [other-key]
                               (binary-do-form prefix did-key other-key instances))
                             all-keys)]
    `(do ~did-do-form
         ~@remaining-forms
         ~(data-accessor prefix did-key instances)
         ~selector-definitions)))
