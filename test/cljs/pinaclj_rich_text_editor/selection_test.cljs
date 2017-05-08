(ns pinaclj-rich-text-editor.selection-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.dom-fixture :as dom-fixture]
            [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(test/use-fixtures :each dom-fixture/clear-dom)

(defn- render [doc-loc]
  (render/render-all (zip/children (zipper/root-loc doc-loc)) (dom-fixture/create-root)))

(defn- perform [doc-loc]
  (selection/on-focus-changed {:doc-loc doc-loc}))

(deftest on-focus-changed []
  (testing "returns root node when there's nothing"
    (is (= ["0" 0 0] (:selection-focus (perform (zipper/->zip [:root {:key "0"}])))))
    (is (= :root (first (zip/node (:doc-loc (perform (zipper/->zip [:root {:key "0"}]))))))))
  (testing "returns the correct focus when a text node is selected"
    (let [doc [:root [:p {:key "parent-key"} "first-text-node" "text-node-with-focus"]]
          doc-loc (zipper/->zip doc)]
      (render doc-loc)
      (let [node (dom-fixture/->text-node (dom-fixture/find-dom-node "parent-key") 1)]
        (dom-fixture/set-range [node 5] [node 5]))
      (is (= ["parent-key" 1 5] (:selection-focus (perform doc-loc))))
      (is (= "text-node-with-focus" (zip/node (:doc-loc (perform doc-loc)))))))
  (testing "selects the first paragraph if no selection is set"
    (let [doc [:root [:p {:key "parent-key"} "first-text-node" "text-node-with-focus"]]
          doc-loc (zipper/->zip doc)]
      (render doc-loc)
      (.removeAllRanges (.getSelection js/document))
      (is (= ["parent-key" 0 0] (:selection-focus (perform doc-loc))))
      (is (= "first-text-node" (zip/node (:doc-loc (perform doc-loc))))))))

(defn- do-move [node focus]
  (-> node
      zipper/->zip
      (selection/move-loc-to-focus focus)
      zip/node))

(deftest move-loc-to-focus []
  (testing "returns based on key"
    (is (= [:p {:key "1"}] (do-move [:root {:key "0"} [:p {:key "1"}]] ["1" nil 0]))))
  (testing "returns right text node"
    (is (= "two" (do-move [:root {:key "0"} "one" "two"] ["0" 1 0]))))
  (testing "returns parent node if the key does not exist"
    (is (= [:root] (do-move [:root] [0 nil 0])))))
