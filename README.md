# Multibranch Tear Down

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fmultibranch-job-tear-down-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/multibranch-job-tear-down-plugin/job/master/)

This plugin only works with Multibranch pipeline projects. When a particular branch is deleted, this plugin will trigger
another job with the `git_url` and `branch_name` of the job being deleted. This way you can perform any cleanup
you desired like bringing down servers.

By default, this plugin looks for a job called `job-tear-down-executor`. You may also specify 
the job to use by going to Manage Jenkins and configuring the Job Name under `MultiBranch Tear Down Plugin`. 
Additionally, you may use `branchTearDownExecutor` in your pipeline to specify a specific job to use for that
branch.

Example in pipeline

```
// Scripted Pipelines
properties([branchTearDownExecutor('my-special-job')])
 
// Declarative Pipelines
options {
  branchTearDownExecutor 'my-special-job'
}
```

License

Released under the MIT license. See [See LICENSE](https://github.com/fuzz-productions/multibranch-tear-down-jenkins-plugin/blob/master/LICENSE) for details.
