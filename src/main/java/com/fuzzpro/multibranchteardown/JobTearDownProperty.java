//
//  JobTearDownProperty.java
//
//  Copyright (c) 2018 Fuzz Productions, LLC (http://fuzzproductions.com/)
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
//

package com.fuzzpro.multibranchteardown;

import hudson.Extension;
import java.io.Serializable;
import jenkins.model.OptionalJobProperty;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class JobTearDownProperty extends OptionalJobProperty<WorkflowJob> implements Serializable {

    private static final long serialVersionUID = 850388417982956491L;
    private final String jobName;

    @DataBoundConstructor
    public JobTearDownProperty(String jobName) {
        this.jobName = jobName;
    }

    @Exported
    public String getJobName() {
        return jobName;
    }

    @Extension
    @Symbol("branchTearDownExecutor")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Trigger a tear down job on deletion?";
        }
    }
}
