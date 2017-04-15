(ns pinaclj-rich-text-editor.render
  (:require [incremental-dom :as idom]))

(defn- select-type [node]
  (if (vector? node)
    (if (= 1 (count node))
      :leaf-element
      :parent-element)
    :text-node))

(defn- ->attr-stream [attrs]
  (mapcat #(list (name (key %)) (val %)) attrs))

(defmulti render-element select-type)

(defn- render-parent [tag attrs children]
  (apply idom/elementOpen (name tag) nil [] (->attr-stream attrs))
  (doall (map render-element children))
  (idom/elementClose (name tag)))

(defmethod render-element :leaf-element [[tag]]
  (idom/elementVoid (name tag) nil []))

(defmethod render-element :parent-element
  ([[tag & [attrs-or-first-child & remaining-children :as all-children]]]
   (if (map? attrs-or-first-child)
     (render-parent tag attrs-or-first-child remaining-children)
     (render-parent tag {} all-children))))

(defmethod render-element :text-node [text]
  (idom/text text))

(defn render [doc node]
  (idom/patch node #(render-element %) doc))
