(ns pinaclj-rich-text-editor.core-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.core :as core]))

(defn- clear-dom [f]
  (let [body (.-body js/document)
        firstChild (.-firstChild body)]
    (when firstChild
      (.removeChild body firstChild)))
  (f))

(test/use-fixtures :each clear-dom)

(defn- raise-key-event [event-name c {:keys [shift meta]}]
  (let [event (.createEvent js/document "Event")]
    (set! (.-charCode event) (.charCodeAt c))
    (set! (.-keyCode event) (.charCodeAt c))
    (set! (.-shiftKey event) shift)
    (set! (.-metaKey event) meta)
    (.initEvent event event-name true true)
    (.dispatchEvent js/document event)))

(defn- find-dom-node [node-key]
  (.querySelector js/document (str "[key='" node-key "']")))

(defn- ->text-node [node]
  (.-firstChild node))

(defn- set-range [node-key text-position]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.removeAllRanges selection)
    (.setStart rng (->text-node (find-dom-node node-key)) text-position)
    (.setEnd rng (->text-node (find-dom-node node-key)) text-position)
    (.addRange selection rng)))

(defn- type-key [c & modifiers]
  (if (raise-key-event "keydown" c modifiers)
    (raise-key-event "keypress" c modifiers)))

(defn- create-root []
  (let [root-element (.createElement js/document "div")]
    (.appendChild (.-body js/document) root-element)))

(defn- perform [initial-doc action]
  (let [editor-node (create-root)]
    (core/attach-editor editor-node initial-doc)
    (action)
    (.-innerHTML editor-node)))

(deftest loading []
  (testing "it renders nodes"
    (core/attach-editor (create-root) [:p {:key "1"} "Rendered"])
    (is (not (nil? (find-dom-node "1"))))))

(deftest typing []
  (testing "typing a character inserts a new paragraph with that element"
    (is (= "<p>C</p>" (perform [:p ""] #(type-key \C :shift true)))))
  (testing "typing a control character does not cause a character to appear"
    (is (= "<p></p>" (perform [:p ""] #(type-key \B :meta true)))))
  (testing "typing the bold character and then text causes bold text to appear"
    (is (= "<p><b>C</b></p>" (perform [:p ""] #(do (type-key \B :meta true)
                                                   (type-key \C :shift true))))))
  (testing "opens a paragraph element if there isn't one already"
    (is (= "<div><p>C</p></div>" (perform [:div] #(type-key \C :shift true))))))

(deftest positioning []
  (testing "text is inserted at last-clicked position"
    (is (= "<p key=\"initial\">Hello, world</p>"
           (perform [:p {:key "initial"} "Hello world"]
                    #(do (set-range "initial" 5)
                         (type-key \,)))))))
