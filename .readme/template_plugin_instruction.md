{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let list = plugins.tools.call('archive_list', { archive: './demo.zip' });
list.result.entries.forEach(e => console.log(e.path, e.size));

let readme = plugins.tools.call('archive_read', { archive: './demo.zip', path: 'README.md' });
console.log(readme.result.text);

let extracted = plugins.tools.call('archive_extract', { archive: './demo.zip' }, {
    outputs: { files: './out/demo' },
});
console.log(extracted.result.filesExtracted, extracted.result.kept);
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
