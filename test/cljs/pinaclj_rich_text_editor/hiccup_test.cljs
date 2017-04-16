(ns pinaclj-rich-text-editor.hiccup-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.hiccup :as hiccup]))

(deftest attr []
  (testing "returns nil when no attribute exists"
    (is (= nil (hiccup/attr [:p] :key))))
  (testing "returns attribute value"
    (is (= 1 (hiccup/attr [:p {:a 1}] :a)))))

(deftest matches-attr? []
  (testing "returns false for no match"
    (is (= false (hiccup/matches-attr? :k :v [:p]))))
  (testing "returns true for match"
    (is (= true (hiccup/matches-attr? :k :v [:p {:k :v}]))))
  (testing "returns false when value is a mismatch"
    (is (= false (hiccup/matches-attr? :k :v [:p {:k 1}])))))
(deftest insert-attr []
  (testing "adds attribute when no attributes exist"
    (is (= [:p {:a 1}] (hiccup/insert-attr [:p] :a 1))))
  (testing "adds attribute when attributes already exist"
    (is (= [:p {:a 1 :b 2}] (hiccup/insert-attr [:p {:a 1}] :b 2))))
  (testing "does not overwrite nodes"
    (is (= [:p {:a 1}] (hiccup/insert-attr [:p {:a 1}] :a 2)))))
