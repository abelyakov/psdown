(ns psdown.core-test
  (:require [clojure.test :refer :all]
            [psdown.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))


(deftest is-goods-cont-test)
(testing "Detect does current div has id goods_cont"
  (let [data {:type :element, :attrs {:id "goods_cont"}, :tag :div, :content []}]
    (is (= true (is-goods-cont data)))))

(deftest is-goods-cont-test)
(testing "Detect does current div has id goods_cont"
  (let [data {:type :element, :attrs {:id "goods_cont2"}, :tag :div, :content []}]
    (is (= false (is-goods-cont data)))))

(deftest find-goods-cont-test)
(testing "get goods-cont div from content"
  (let [data [["a" 2 3] {:type :element, :attrs {:id "goods_cont"}, :tag :div, :content []}]]
    (is (not-empty (extract-goods-cont data)))))


