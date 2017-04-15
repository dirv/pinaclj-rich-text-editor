(ns pinaclj-rich-text-editor.render
  (:require [incremental-dom :as idom]))

(defn- select-type [node]
  (if (vector? node)
    (if (= 1 (count node))
      :leaf-element
      :parent-element)
    :text-node))

(defmulti render-element select-type)
(defmethod render-element :leaf-element [[tag]]
  (idom/elementVoid (name tag) nil []))

(defmethod render-element :parent-element [[tag child]]
  (idom/elementOpen (name tag))
  (render-element child)
  (idom/elementClose (name tag)))

(defmethod render-element :text-node [text]
  (idom/text text))

(defn render [doc node]
  (idom/patch node #(render-element %) doc))
