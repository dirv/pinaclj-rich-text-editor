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

(defn- ->text-node [node index]
  (nth (array-seq (.-childNodes node)) index))

(defn- set-range
  ([node-key text-position]
  (set-range node-key 0 text-position))
  ([node-key text-node text-position]
  (let [selection (.getSelection js/document)
        rng (.createRange js/document)]
    (.removeAllRanges selection)
    (.setStart rng (->text-node (find-dom-node node-key) text-node) text-position)
    (.collapse rng true)
    (.addRange selection rng))))

(defn- type-key [c & modifiers]
  (if (raise-key-event "keydown" c modifiers)
    (raise-key-event "keypress" c modifiers)))

(defn- create-root []
  (let [root-element (.createElement js/document "div")]
    (.appendChild (.-body js/document) root-element)))

(defn- remove-all-keys [node]
  (when-not (core/text-node? node)
    (.removeAttribute node "key")
    (doall (map remove-all-keys (array-seq (.-childNodes node))))))

(defn- perform [initial-children action]
  (let [editor-node (create-root)]
    (apply core/attach-editor editor-node initial-children)
    (action)
    (remove-all-keys editor-node)
    (.-innerHTML editor-node)))

(deftest loading []
  (testing "it renders nodes"
    (core/attach-editor (create-root) [:p {:key "1"} "Rendered"])
    (is (not (nil? (find-dom-node "1")))))
  (testing "renders multiple nodes"
    (let [root-node (create-root)]
      (core/attach-editor root-node [:p "a"] [:p "b"])
      (remove-all-keys root-node)
      (is (= "<p>a</p><p>b</p>" (.-innerHTML root-node))))))

(deftest typing []
  (comment (testing "typing a character inserts that character into existing paragraph"
    (is (= "<p>C</p>" (perform [[:p ""]] #(type-key \C :shift true))))))
  (comment(testing "typing a control character does not cause a character to appear"
    (is (= "<p></p>" (perform [[:p ""]] #(type-key \B :meta true))))))
  (comment (testing "typing the bold character and then text causes bold text to appear"
    (is (= "<p><b>C</b></p>" (perform [[:p ""]] #(do (type-key \B :meta true)
                                                   (type-key \C :shift true)))))))
  (comment (testing "opens a paragraph element if there isn't one already"
    (is (= "<p>C</p>" (perform [] #(type-key \C :shift true)))))))

(deftest positioning []
  (comment (testing "text is inserted at last-clicked position"
    (is (= "<p>Hello, world</p>"
           (perform [[:p {:key "initial"} "Hello world"]]
                    #(do (set-range "initial" 5)
                         (type-key \,)))))))
  (testing "positions when text node is the third child"
    (is (= "<p><b>Hello</b> world!</p>"
           (perform [[:p {:key "1"} [:b {:key "2"} "Hello"] " world"]]
                    #(do (set-range "1" 1 6)
                         (type-key \!)))))))

