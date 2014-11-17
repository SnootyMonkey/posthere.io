# POSThere.io - Debug all the POST requests!

[POSThere.io](http://posthere.io/) is a simple service for developers to help debug API calls and web hooks. 

As a developer, if your code is POSTing to someone else’s code, either to their API, or via a web-hook from your own API… it can be hard to iterate and validate your code without the burden of test mocking (tedious and not a complete test) or servicing your own POST requests (wasted code and effort).

Wouldn’t it be nice to be able to instantly see what your code POSTed at any time in as easy, friendly browser interface? Now you can. Just remember one URL: [posthere.io](http://posthere.io/). 

Decide if you want to use HTTP or HTTPs, and come up with a string that makes sense for the POST you’re attempting, and viola! 

**https**://posthere.io/**test-my-twitter-code**

**http**://posthere.io/**a-webhook-callback-test**

**http**://posthere.io/**another-webhook-callback-test/with/complexity?api=123**

After POSTing your JSON, XML, or form fields, point your web browser at the same URL and you’ll see a clear history and details of all the POST requests you made. Neat, huh?

Need to simulate different HTTP responses to your POST, to ensure your code can handle them? It’s easy:

**https**://posthere.io/**test-my-twitter-code**?status=**201**

**http**://posthere.io/**a-webhook-callback-test**?status=**500**

**http**://posthere.io/**another/test/with/complexity?api=123**&status=**404**

Don’t trust us with your test data? No need to, it’s open source! Check the code and verify we don’t do anything nefarious, or run it yourself [internally](run-it-yourself), or on [Heroku](#run-it-on-heroku).

Need more features? [Fork it](https://github.com/path/posthere.io/fork)! It’s all [well-tested]() Clojure and ClojureScript. Send us a [pull request](#development-and-contributing) when you're done.

## Usage

99.9% of users should use [POSThere.io](http://posthere.io/) by just going to the [website](http://posthere.io/). The website will provide you with a unique URL, or you can customize it, and you can use the provided URL to test your API requests or webhook notifications.

Once one or more requests have been made, simply visit the URL in your browser and you'll see a display of the requests that POSThere.io received.

## Limits

Given the nature of POSThere.io as a free, shared resource, there are some limits. You can certainly host your own copy and remove any of these limits if they are problematic:

* POSThere.io captures the results of your POST requests, but does not capture GET, PUT, PATCH, or DELETE or any other HTTP requests.
* The body of the request is limited to 1MB or less.
* Only the last 100 requests per unique URL are captured.
* Captured requests are only kept for 24 hours.
* Single and multi-part file uploads are not supported.
* Only JSON, XML and URL encoded form fields are rendered nicely, other data formats are not.
* The response to the POST request is fixed and cannot be adjusted.
* You can't request simulated timeout or delay to the request.
* There's no API to programmatically retrieve the requests that were made.

### Run it Yourself

If you want to host it internally yourself, or you want to hack on the Clojure/ClojureScript code, you'll need to install [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html), and [Leiningen](http://leiningen.org/).

Then you can build the ClojureScript code:

```console
lein cljsbuild once
```

And you can start a development server:

```console
lein run 
```

Then you can visit your very own POSThere.io in your browser at [http://localhost:3000/](http://localhost:3000/).

### Run it on Heroku

TBD.

## Testing

To run the tests:

```console
lein midje
```


## Development and Contributing

If you'd like to enhance POSThere.io, please fork [POSThere.io on GitHub](https://github.com/path/posthere.io). If you'd like to contribute back your enhancements (awesome!), please submit your pull requests to the `dev` branch. We promise to look at every pull request and incorporate it, or at least provide feedback on why if we won't.

* Do your best to conform to the coding style that's here... We like it.
* Use 2 soft spaces for indentation.
* Don't leave trailing spaces after lines.
* Don't leave trailing new lines at the end of files.
* Write comments.
* Write tests.
* Don't submit über pull requests, keep your changes atomic.
* Have fun!


## License

POSThere.io is distributed under the [Mozilla Public License v2.0](http://www.mozilla.org/MPL/2.0/).

Copyright © 2014 Path, Inc.