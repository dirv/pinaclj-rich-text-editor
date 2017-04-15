(ns pinaclj-rich-text-editor.core-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.core :as core]))

(defn- type-key [c & {:keys [shiftKey]}]
  (let [event (.createEvent js/document "Event")]
    (set! (.-charCode event) (.charCodeAt c))
    (set! (.-shiftKey event) shiftKey)
    (.initEvent event "keypress" true true)
    (.dispatchEvent js/document event)))

(defn- perform-action [initial-doc action]
  (let [editor-node (.createElement js/document "div")]
    (core/load-doc initial-doc)
    (core/attach-editor editor-node)
    (action)
    (core/render editor-node)
    (.-innerHTML editor-node)))

(deftest key-press []
  (testing "typing a character inserts a new paragraph with that element"
    (is (= "<p>C</p>" (perform-action [:p ""] #(type-key \C :shift true))))))
