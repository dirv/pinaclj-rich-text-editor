(ns pinaclj-rich-text-editor.selection-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.dom-fixture :as dom-fixture]
            [pinaclj-rich-text-editor.render :as render]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(test/use-fixtures :each dom-fixture/clear-dom)

(defn- render [doc-loc]
  (render/render-all (zip/children (zipper/root-loc doc-loc)) (dom-fixture/create-root))
  (selection/on-focus-changed doc-loc))

(deftest on-focus-changed []
  (testing "returns root node when there's nothing"
    (is (= ["0" 0 0] (:selection-focus (selection/on-focus-changed (zipper/->zip [:root {:key "0"}])))))
    (is (= :root (first (zip/node (:doc-loc (selection/on-focus-changed (zipper/->zip [:root {:key "0"}]))))))))
  (testing "returns the correct focus when a text node is selected"
    (let [doc [:root [:p {:key "parent-key"} "first-text-node" "text-node-with-focus"]]
          doc-loc (zipper/->zip doc)]
      (render doc-loc)
      (dom-fixture/set-range (dom-fixture/->text-node (dom-fixture/find-dom-node "parent-key") 1) 5)
      (is (= ["parent-key" 1 5] (:selection-focus (selection/on-focus-changed doc-loc))))
      (is (= :p (first (zip/node (:doc-loc (selection/on-focus-changed doc-loc))))))))
  (testing "selects the first paragraph if no selection is set"
    (let [doc [:root [:p {:key "parent-key"} "first-text-node" "text-node-with-focus"]]
          doc-loc (zipper/->zip doc)]
      (render doc-loc)
      (.removeAllRanges (.getSelection js/document))
      (is (= ["parent-key" 0 0] (:selection-focus (selection/on-focus-changed doc-loc))))
      (is (= :p (first (zip/node (:doc-loc (selection/on-focus-changed doc-loc)))))))))
