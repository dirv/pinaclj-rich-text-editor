(ns pinaclj-rich-text-editor.toggles-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.toggles :as toggles]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(deftest initialize []
  (is (= {} (:toggles (toggles/initialize {})))))

(deftest toggle []
  (testing "toggles strong"
    (is (true? (:strong (:toggles (toggles/toggle {:toggles {}} #{:B :meta}))))))
  (testing "does not toggle for other key strokes"
    (is (not (:strong (:toggles (toggles/toggle {:toggles {}} #{:C :meta}))))))
  (testing "toggles off if it's already on"
    (is (false? (:strong (:toggles (toggles/toggle {:toggles {:strong true}} #{:B :meta}))))))
  (testing "toggles em"
    (is (true? (:em (:toggles (toggles/toggle {:toggles {}} #{:I :meta})))))))

(def empty-doc [:root])
(def strong-doc [:root [:p {:key 0} [:strong {:key 1} "A"]]])
(def strong-em-doc [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} ""]]]])
(def strong-em-text-doc [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} "test node"]]]])
(def text-doc [:root [:p {:key 1} "test node"]])
(def strong-text-doc [:root [:p {:key 1} [:strong {:key 2} "test node"]]])


(defn- next-key-from [n]
  (let [next-key (atom n)]
    (fn []
      (let [new-key @next-key]
        (swap! next-key inc)
        new-key))))

(defn- focus [state]
  (assoc state
         :doc-loc
         (selection/move-loc-to-focus (zipper/->zip (:doc state)) (:selection-focus state))))

(defn- perform [state c]
  (-> (focus state)
      (toggles/handle c)))

(defn- doc [state]
  (-> state
      (:doc-loc)
      zip/root))

; todo - need to test selection focus has been updated too
(deftest handle []
  (testing "inserts tag if it is not already in one"
    (let [state {:toggles {:strong true}
                 :doc empty-doc
                 :selection-focus [0 nil 0]
                 :next-key-fn (fn [] 5)}]
     (is (= [:strong {:key 5}] (zip/node (:doc-loc (perform state \X)))))))

  (testing "does not insert tag if it isn't toggled"
    (is (= empty-doc
           (doc (perform {:toggles {}
                          :doc empty-doc
                          :select-focus [0 0 0]} \X)))))

  (testing "does not insert tag if it's already in one"
    (is (= strong-doc
           (doc (perform {:toggles {:strong true}
                          :doc strong-doc
                          :selection-focus [1 0 1]
                          :next-key-fn (fn [] 5)} \X)))))

  (testing "does not insert tag if that tags appears somewhere higher in the tree"
    (is (= strong-em-doc
           (doc (perform {:toggles {:strong true
                                    :em true}
                          :doc strong-em-doc
                          :selection-focus [2 0 0]
                          :next-key-fn (fn [] 5)} \X)))))

  (testing "moves out of the tag if it has been turned off but currently in it"
    (let [state {:toggles {:strong false}
                 :doc strong-doc
                 :selection-focus [1 0 1]
                 :next-key-fn (fn [] 5)}]
      (is (= [0 1 0]
           (:selection-focus (perform state \X))))))

  (testing "splits a text node when inserting tag"
    (let [state (perform {:toggles {:strong true}
                          :doc text-doc
                          :selection-focus [1 0 4]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test" [:strong {:key 2}] " node"]] (doc state)))
      (is (= [2 nil 0] (:selection-focus state)))))

  (testing "does not return empty text nodes when splitting at lhs"
    (let [state (perform {:toggles {:strong true}
                          :doc text-doc
                          :selection-focus [1 0 0]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2}] "test node"]] (doc state)))))

  (testing "does not return empty text nodes when splitting at rhs"
    (let [state (perform {:toggles {:strong true}
                          :doc text-doc
                          :selection-focus [1 0 9]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test node" [:strong {:key 2}]]] (doc state)))))

  (testing "splitting a node when leaving a tag"
    (let [state (perform {:toggles {:strong false}
                          :doc strong-text-doc
                          :selection-focus [2 0 4]
                          :next-key-fn (fn [] 3)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2} "test"] "" [:strong {:key 3} " node"]]] (doc state)))
      (is (= [1 1 0] (:selection-focus state)))))

  (testing "creates empty nodes when leaving a strong tag at lhs"
    (let [state (perform {:toggles {:strong false}
                          :doc strong-text-doc
                          :selection-focus [2 0 0]} \X)]
      (is (= [:root [:p {:key 1} "" [:strong {:key 2} "test node"]]] (doc state)))
      (is (= [1 0 0] (:selection-focus state)))))

  (testing "creates empty nodes when leaving a strong tag at rhs"
    (let [state (perform {:toggles {:strong false}
                          :doc strong-text-doc
                          :selection-focus [2 0 9]} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2} "test node"] ""]] (doc state)))
      (is (= [1 1 0] (:selection-focus state)))))

  (testing "recreates in-between nodes when closing this one"
    (let [state (perform {:toggles {:strong false
                                    :em true}
                          :doc strong-em-doc
                          :selection-focus [2 0 0]} \X)]
      (is (= [:root [:p {:key 0} [:em {:key 2} ""]]] (doc state)))
      (is (= [2 0 0] (:selection-focus state)))))

  (testing "it splits nodes correctly when in the middle of chain"
    (let [state (perform {:toggles {:strong false
                                    :em true}
                          :doc strong-em-text-doc
                          :selection-focus [2 0 4]
                          :next-key-fn (next-key-from 3)} \X)]
      (is (= [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} "test"]] [:em {:key 3} ""] [:strong {:key 4} [:em {:key 5} " node"]]]] (doc state)))))

  (testing "opens both strona and em tags"
    (let [state (perform {:toggles {:strong true
                                    :em true}
                          :doc [:root [:p {:key 1}]]
                          :selection-focus [1 nil 0]
                          :next-key-fn (next-key-from 2)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2} [:em {:key 3}]]]] (doc state))))))
