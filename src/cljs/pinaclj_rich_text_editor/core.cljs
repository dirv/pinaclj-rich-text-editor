(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.render :as render]))

(def doc (atom []))
(def bold (atom false))

(defn- insert-character [c doc]
  (if @bold
    [:p [:b (str (nth doc 1) c)]]
    [:p (str (nth doc 1) c)]))

(defn- handle-keypress [e]
  (swap! doc
         (partial insert-character (char (.-charCode e)))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (reset! bold true)
    (.preventDefault e)))

(defn attach-editor [root]
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress))

(defn load-doc [new-doc]
  (reset! doc new-doc))

(defn render [node]
  (render/render @doc node))

(enable-console-print!)
