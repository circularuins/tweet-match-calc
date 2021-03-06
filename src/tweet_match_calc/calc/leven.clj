(ns tweet-match-calc.calc.leven)

(defn levenshtein-distance [x y]
  (last
   (reduce
    (fn [prev j]
      (reduce
       (fn [curr i]
         (conj curr
               (+ 1 (min (nth prev (+ i 1))
                         (nth curr i)
                         (- (nth prev i)
                            (if (= (nth x i) (nth y j)) 1 0))))))
       [(+ j 1)] (range (count x))))
    (range (+ 1 (count x)))
    (range (count y)))))
