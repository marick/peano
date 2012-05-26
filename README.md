The goal of this project is to make it as easy as possible
to construct test data, including hierarchical data with
constraints on which datum X can be used with which datum
Y. 

## Easy lookup of named tabular data

So, for example, suppose I have animals and medical procedures:

```clojure
(data [animal :by :name :with-selectors]
      {:name "betty" :species :bovine :legs 4}
      {:name "hank" :species :equine :legs 4}
      {:name "jake" :species :equine :legs 3} ; poor jake
      {:name "dawn" :species :human :legs 2})

(data [procedure :by :name :with-selectors]
      {:name "hoof trim" :species-rule :equine-only :days-delay 0}
      {:name "superovulation" :species-rule :bovine-only :days-delay 90}
      {:name "physical exam" :species-rule :all :days-delay 0})
```

From this, we can auto-construct `selectors` that let us
easily ask for the names of data that satisfy constraints.

```clojure
  (animal?> :legs 4) ;; ["betty" "hank"]
  (one-procedure?> {:days-delay 0}) ;; "hoof trim"
```

Once you have names, you can use the tables to populate a
database or whatever:

```clojure
  (populate-test-database (animal-data (one-animal?>)))
```

This part works.

## Groups of names that satisfy constraints.

Let's say the `permitted??` relation/goal succeeds when a
particular procedure is permitted on a particular
animal. So, for example:

```clojure
(permitted?? "hoof trim" "hank") ;; succeed
(permitted?? "hoof trim" "dawn") ;; fail
```

Now we can ask for pairs that are permitted:

```clojure
(permitted?>) ;; [["hoof trim", "hank"], ["superovulation" "betty"]], etc.

(one-permitted?> :procedure "hoof trim") ;; ["hoof trim" "hank"], etc.
```

Some of this works. I'm undecided on the API. (Should it
return an array or a map? If you specify a constraint like
"I want only hoof trims", should "hoof trim" appear in the
output?

## Hierarchical data

The app from which these examples are drawn is largely
about *reservations*, which are sets of *groups*, each of
which is a particular (permitted) procedure/animal pair. I
want to show a "picture" of the "shape" of reservation I
want, and then let an engine fill in the blanks.

For example, the most common reservation used in the tests
(because it's the only easy one to create) is one group with
one procedure/animal pair. In many many cases, I don't
actually care which procedure and which animal I use. I want
to describe it like this:

```clojure
[[- -]]
```

... and get back a data structure something like this:

```clojure
[:reservation [[["hoof trim" "hank"]]]]
```

Then a database-population routine (that would look
extremely like the one that already exists to add a
reservation from json data) would take over.

Sometimes you'd want constraints. Some of those can be
expressed in abbreviated form. Suppose I want two pairs, the
first containing hank and some appropriate procedure, the
second containing a cow. That would look like this:

```clojure
[[- "hank"] [- {:species :bovine}]]
```

I have a prototype of this working. The big question is can
it be turned into a [stunted framework](http://www.artima.com/weblogs/viewpost.jsp?thread=8826): one that's not so
universal that it's too hard to learn and too fiddly to work
with, but that doesn't require so much customization that
it's not worthwhile?

Another question is: is logic programming too esoteric for
average people to use?

And: is Clojure a large enough language community? one that
builds the sort of apps that need sophisticated test data generation?

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
