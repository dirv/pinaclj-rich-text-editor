(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.dom :as dom]
            [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def doc-loc (atom nil))
(def selection-focus (atom nil))
(def node-key (atom 0))

(defn- next-key []
  (let [next-key @node-key]
    (swap! node-key inc)
    (str next-key)))

(defn- handle-keypress [e]
  (let [new-state (selection/on-focus-changed @doc-loc)]
    (reset! doc-loc (:doc-loc new-state))
    (reset! selection-focus (:selection-focus new-state)))
  (swap! doc-loc editor/insert-into-loc @selection-focus (char (.-charCode e))))

(defn- handle-keydown [e]
  (when (.-metaKey e)
    (editor/toggle-bold)
    (.preventDefault e)))

(defn- assign-key-to-node [node]
  (if (vector? node) (hiccup/insert-attr node :key (next-key)) node))

(defn- assign-keys [loc]
  (zipper/map-loc loc assign-key-to-node))

(defn- render-into [root]
  (when-let [children (zip/children (zipper/root-loc @doc-loc))]
    (render/render-all children root)))

(defn attach-editor [root & children]
  (editor/reset)
  (reset! doc-loc (-> (apply vector :root children) zipper/->zip assign-keys))
  (reset! node-key 0)
  (render-into root)
  (selection/select-first-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress)
  (.addEventListener js/document "keypress" (fn [e] (render-into root))))

(enable-console-print!)
