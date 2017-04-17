(ns pinaclj-rich-text-editor.core
  (:require [pinaclj-rich-text-editor.dom :as dom]
            [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [pinaclj-rich-text-editor.strong-text :as strong-text]
            [clojure.zip :as zip]))

(def state (atom {}))

(defn- build-next-key-fn []
  (let [node-key (atom 0)]
    (fn []
      (let [next-key @node-key]
        (swap! node-key inc)
        (str next-key)))))

(defn- handle-keypress [e]
  (swap! state selection/on-focus-changed)
  (swap! state editor/insert-into-loc (char (.-charCode e))))

(defn- handle-keydown [e]
  (when-let [new-state (strong-text/toggle @state e)]
    (reset! state new-state)
    (.preventDefault e)))

(defn- assign-key-to-node [next-key-fn node]
  (if (vector? node) (hiccup/insert-attr node :key (next-key-fn)) node))

(defn- assign-keys [{loc :doc-loc next-key-fn :next-key-fn :as state}]
  (assoc state :doc-loc (zipper/map-loc loc (partial assign-key-to-node next-key-fn))))

(defn- render-into [root]
  (when-let [children (zip/children (zipper/root-loc (:doc-loc @state)))]
    (render/render-all children root)))

(defn attach-editor [root & children]
  (reset! state {:doc-loc (-> (apply vector :root children) zipper/->zip)
                 :selection-focus ["0" 0 0]
                 :next-key-fn (build-next-key-fn)})
  (swap! state assign-keys)
  (swap! state editor/initialize)
  (render-into root)
  (selection/select-first-node root)
  (.addEventListener js/document "keydown" handle-keydown)
  (.addEventListener js/document "keypress" handle-keypress)
  (.addEventListener js/document "keypress" (fn [e] (render-into root))))

(enable-console-print!)
