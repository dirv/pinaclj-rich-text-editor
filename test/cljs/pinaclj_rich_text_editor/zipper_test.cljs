(ns pinaclj-rich-text-editor.zipper-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [clojure.zip :as zip]
            [pinaclj-rich-text-editor.zipper :as zipper]))

(deftest zip-navigation []
  (let [root-loc (zipper/->zip [:p [:em "A"] [:strong [:em "B"] "C"] "D"])]
    (testing "gets root tag correct"
      (is (= :p (first (zip/node root-loc)))))
    (testing "moves down when going to next with no neighbours"
      (is (= [:em "A"] (-> root-loc zip/next zip/node))))
    (testing "moves to next neighbour"
      (is (= "A" (-> root-loc zip/next zip/next zip/node))))))

(deftest zip-insertion []
  (testing "can insert nodes"
    (is (= [:p "A"] (-> [:p] zipper/->zip (zip/insert-child "A") zip/node))))
  (testing "can insert children into attribute nodes"
    (is (= [:p {:a 1} "B"] (-> [:p {:a 1}] zipper/->zip (zip/insert-child "B") zip/node))))
  (testing "can insert text children"
    (is (= [:div [:p ""]] (-> [:div [:p]] zipper/->zip zip/down (zip/insert-child "") zip/root)))))

(deftest find-loc []
  (testing "returns nil for no match"
    (is (= nil (-> [:p "root"] zipper/->zip (zipper/find-loc #(= (first %) :em))))))
  (testing "finds matching root node"
    (is (= [:p "root"]
           (-> [:p "root"] zipper/->zip (zipper/find-loc #(= (first %) :p)) zip/node))))
  (testing "finds matching child node"
    (is (= [:em "child"]
           (-> [:p [:em "child"]] zipper/->zip (zipper/find-loc #(= (first %) :em)) zip/node))))
  (testing "finds child even if the zipper is positioned after it"
    (is (= [:p "childA"]
           (-> [:p [:p "childA"] [:p "childB"]] zipper/->zip (zipper/find-loc #(= (second %) "childA")) zip/node)))))


(deftest split-node []
  (testing "returns two text strings if splitting a text node"
    (let [parent-loc (zipper/->zip [:root "text-loc"])
          text-node-loc (zip/down parent-loc)]
      (is (= [[:root "text"] [:root "-loc"]] (zipper/split-node parent-loc text-node-loc 4)))))
  (testing "includes parent node"
    (let [parent-loc (zip/down (zipper/->zip [:root [:p "text-loc"]]))
          text-node-loc (zip/down parent-loc)]
      (is (= [[:p "text"] [:p "-loc"]] (zipper/split-node parent-loc text-node-loc 4)))))
  (testing "removes nodes on left side"
    (let [parent-loc (zip/down (zipper/->zip [:root [:p [:strong "test"] [:em "-loc"]]]))
          text-node-loc (-> parent-loc zip/down zip/right zip/down)]
      (is (= [[:p [:strong "test"] [:em "-"]] [:p [:em "loc"]]] (zipper/split-node parent-loc text-node-loc 1)))))
  (testing "removes nodes on right side"
    (let [parent-loc (zip/down (zipper/->zip [:root [:p [:em "-loc"] [:strong "test"]]]))
          text-node-loc (-> parent-loc zip/down zip/down)]
      (is (= [[:p [:em "-"]] [:p [:em "loc"] [:strong "test"]]] (zipper/split-node parent-loc text-node-loc 1))))))
