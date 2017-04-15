(ns pinaclj-rich-text-editor.render
  (:require [incremental-dom :as idom]))

(defn- render-element [[tag content]]
  (idom/elementOpen (name tag))
  (cond
    (string? content)
    (idom/text content)
    (vector? content) (render-element content))
  (idom/elementClose (name tag)))

(defn render [doc node]
  (idom/patch node #(render-element %) doc))


